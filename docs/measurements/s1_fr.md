# Mesures — semaine 1

## Environnement

| | |
| --- | --- |
| Machine | MacBook, Apple M2 Pro, 16 Go de RAM |
| PostgreSQL | 17.11 (Docker, `postgres:17`, aarch64) |
| Réglages | `shared_buffers=1GB`, `work_mem=32MB`, `maintenance_work_mem=512MB`, `max_wal_size=4GB` |
| Données | 1 M utilisateurs, 1,2 M comptes, 5 M transactions (`db/02-seed.sql`) |

---

## Lundi 5 — Génération des données et tailles

### Temps de génération

| Étape | Lignes | Temps | Débit |
| --- | ---: | ---: | ---: |
| `users` | 1 000 000 | 6,3 s | ~160 k lignes/s |
| `accounts` (EUR) | 1 000 000 | 4,3 s | ~230 k lignes/s |
| `accounts` (USD, 20 %) | 200 000 | 0,9 s | ~215 k lignes/s |
| `transactions` | 5 000 000 | 48,6 s | ~100 k lignes/s |
| `ANALYZE` | — | 0,8 s | — |

`transactions` est la plus lente par ligne : chaque insertion vérifie deux clés étrangères (`from_account_id`, `to_account_id`).

### Volumétrie

Vérifiée par `count(*)` : 1 000 000 utilisateurs, 1 200 000 comptes, 5 000 000 transactions.

| Table | Table | Index | Total | Octets/ligne (total) |
| --- | ---: | ---: | ---: | ---: |
| `transactions` | 403 MB | 107 MB | 510 MB | ~107 |
| `users` | 89 MB | 89 MB | 178 MB | ~187 |
| `accounts` | 88 MB | 26 MB | 113 MB | ~99 |

Détail des index (à ce stade, seulement les clés primaires et l'unicité) :

| Index | Taille |
| --- | ---: |
| `transactions_pkey` | 107 MB |
| `users_email_key` | 68 MB |
| `accounts_pkey` | 26 MB |
| `users_pkey` | 21 MB |

Requête utilisée :

```sql
SELECT relname,
       pg_size_pretty(pg_relation_size(relid))       AS table_size,
       pg_size_pretty(pg_indexes_size(relid))        AS indexes_size,
       pg_size_pretty(pg_total_relation_size(relid)) AS total
FROM pg_catalog.pg_statio_user_tables
ORDER BY pg_total_relation_size(relid) DESC;
```
```sql
SELECT indexname, indexdef
FROM pg_indexes
WHERE tablename = 'table_name';
```
### Observations

- Une transaction pèse ~85 octets dans la table, dont ~24 octets d'en-tête de ligne (avec `xmin`/`xmax`) : c'est le coût du MVCC.
- L'index unique sur `email` (`text`) pèse 3 fois l'index de clé primaire de `users` (`bigint`) : la taille d'un index dépend de la largeur de la clé.
- Les clés étrangères ne sont pas encore indexées (sujet de mardi) : les tailles par ligne vont augmenter.

### Extrapolation à 65 M d'utilisateurs

**Tables proportionnelles au nombre d'utilisateurs** (règle de trois sur le total) :

| Table | 1 M utilisateurs | 65 M utilisateurs |
| --- | ---: | ---: |
| `users` | 178 MB | ~11,5 GB |
| `accounts` | 113 MB | ~7 GB |

**`transactions` : grossit avec le temps, pas avec le nombre d'utilisateurs.** Base : ~107 octets par transaction (table + clé primaire).

| Hypothèse | Transactions | Taille |
| --- | ---: | ---: |
| H1 — stock : 5 transactions par utilisateur au total | 325 M | ~33 GB |
| H2 — flux : 10 transactions par utilisateur et par mois | 650 M / mois | ~66 GB / mois, **~800 GB / an** |

H1 est une photo à un instant donné et sous-estime le problème. H2 est le bon raisonnement : le volume dépend de l'activité et de la durée de conservation.

### Conclusion pour la question 1

Les tables liées au nombre d'utilisateurs tiennent en ~20 GB : 65 M d'utilisateurs, ce n'est pas le problème. Le vrai sujet, ce sont les transactions : ~0,8 TB par an avant les index secondaires, donc plusieurs TB après quelques années. C'est là qu'interviennent le partitionnement par date (jeudi), l'archivage et la durée de rétention.

---

## Mardi 6 — Historique d'un compte

### Méthode

- Index : `db/03-index.sql`

  ```sql
  CREATE INDEX idx_tx_from_created  ON transactions (from_account_id, created_at);
  CREATE INDEX idx_tx_to_created    ON transactions (to_account_id,   created_at);
  CREATE INDEX idx_accounts_user ON accounts (user_id);
  ```

- `EXPLAIN (ANALYZE, BUFFERS)`, chaque requête lancée 3 fois, **dernière exécution retenue** (cache chaud). Les temps sont l'`Execution Time` côté serveur.
- Mesures « sans index » : index présents mais désactivés pour la session (`SET enable_indexscan = off; SET enable_bitmapscan = off;`).
- Compte chaud : **42** (1 370 transactions). Compte calme : **900000** (6 transactions).
- Les buffers sont des pages de 8 Ko. `hit` = trouvée dans les shared buffers de PostgreSQL ; `read` = demandée au système (cache de l'OS ou disque).

### Requêtes

- **A.** Historique, version `OR` : `WHERE from_account_id = ? OR to_account_id = ? ORDER BY created_at DESC LIMIT 50`.
- **B.** Historique, version `UNION ALL` : chaque branche lit les 50 lignes les plus récentes directement dans son index.
- **C.** Transactions sortantes d'un utilisateur : jointure `accounts` → `transactions` filtrée sur `accounts.user_id`.

Le SQL complet et les plans sont dans [`s1.md`](s1.md) (section « Tuesday 6 » et annexe).

### Résultats

| # | Requête | Compte | Nœuds principaux | Temps d'exécution | Buffers |
| --- | --- | --- | --- | ---: | ---: |
| 1 | A `OR`, sans index | 42 | Parallel Seq Scan + tri top-N | 143,6 ms | 51 621 |
| 2 | A `OR`, sans index | 900000 | Parallel Seq Scan + tri | 217,9 ms | 51 621 |
| 3 | A `OR`, avec index | 42 | BitmapOr + Bitmap Heap Scan + tri top-N | 11,7 ms | 1 356 |
| 4 | A `OR`, avec index | 900000 | BitmapOr + Bitmap Heap Scan + tri | 0,24 ms | 12 |
| 5 | B `UNION ALL`, avec index | 42 | Merge Append + 2 × Index Scan Backward | 1,96 ms | 57 |
| 6 | B `UNION ALL`, avec index | 900000 | Merge Append + 2 × Index Scan Backward | 0,17 ms | 14 |
| 7 | C jointure, sans index sur `user_id` | — | Parallel Seq Scan sur `accounts` | 59,9 ms | 11 224 |
| 8 | C jointure, avec index sur `user_id` | — | Index Scan sur `accounts` | 0,70 ms | 13 |

Compte chaud, de « sans index » à la réécriture (1 → 5) : **73 fois plus rapide, 900 fois moins de pages lues**.

Variabilité sans index (cache froid → chaud) : 437,8 / 369,2 / 143,6 ms pour le compte 42, 366,5 / 219,6 / 217,9 ms pour le compte 900000. La ligne 8 est une première exécution (`read=3`) : les pages de l'index n'étaient pas encore en cache.

### Observations

1. **Sans index, chaque recherche lit toute la table.** 51 621 pages × 8 Ko = 403 Mo, exactement la taille de `transactions` mesurée lundi. Le compte chaud et le compte calme coûtent pareil : le travail dépend de la taille de la table, pas de celle du résultat.
2. **Le nombre de `read` reste à ~42 500 même à la troisième exécution.** Pour un parcours séquentiel d'une table plus grande qu'un quart de `shared_buffers` (403 Mo > 256 Mo), PostgreSQL utilise un petit tampon circulaire, pour qu'un gros parcours ne vide pas le cache. Le gain de 438 à 144 ms vient du cache de l'OS, pas de celui de PostgreSQL.
3. **Le `OR` avec index lit quand même tout l'historique du compte.** 1 370 lignes sont lues pour en garder 50, et `Heap Blocks: exact=1345` signifie presque une page par ligne : les transactions d'un compte sont éparpillées dans la table, car insérées au fil du temps. Un tri top-N suit.
4. **Le `UNION ALL` ne lit que ce qu'il renvoie.** Chaque branche parcourt son index à l'envers et s'arrête après 50 entrées ; pas de tri, car l'index est déjà ordonné par `created_at` à l'intérieur d'un compte. 57 pages au lieu de 1 356 (24 fois moins), 1,96 ms au lieu de 11,7 ms (6 fois plus rapide).
5. **Un index croissant sert une requête décroissante.** Les index sont sur `created_at` croissant et le plan affiche `Index Scan Backward` : un B-tree se lit dans les deux sens, le `DESC` dans la définition de l'index n'est pas nécessaire ici.
6. **Sur un compte calme, les deux versions se valent** (0,24 ms et 12 pages contre 0,17 ms et 14 pages). La réécriture ne rapporte que pour les comptes à long historique, précisément ceux qui posent problème en production.
7. **Les clés étrangères non indexées sont un piège classique.** PostgreSQL indexe automatiquement les clés primaires, pas les clés étrangères. Trouver les comptes d'un utilisateur lisait toute la table `accounts` : 11 224 pages ≈ 88 Mo, sa taille mesurée lundi. Avec l'index : 13 pages, 85 fois plus rapide.

### Expérience complémentaire : historique complet, sans `ORDER BY` ni `LIMIT`

Mêmes requêtes `OR` et `UNION ALL`, qui renvoient toutes les lignes du compte. Une seule exécution chacune : chiffres indicatifs.

| Version | Compte | Sans index | Avec index |
| --- | --- | ---: | ---: |
| `OR` | 42 | 315,0 ms (1 Parallel Seq Scan) | 35,3 ms (1 356 buffers) |
| `UNION ALL` | 900000 | 402,9 ms (2 Parallel Seq Scans) | 1,6 ms (12 buffers) |

**Constat :** sans index, le `UNION ALL` est plus lent que le `OR`, car il parcourt la table deux fois (une par branche) au lieu d'une. La réécriture ne vaut que si chaque branche peut utiliser un index.

Remarque : les deux versions ont été lancées sur des comptes différents. Pour un parcours séquentiel, cela ne change rien (toute la table est lue) ; avec index, les chiffres ne sont pas comparables.

Les temps côté client de pgAdmin (« Query complete », de 0,06 à 0,93 s) ont été écartés : ils incluent le réseau et l'affichage, pgAdmin s'arrête à 1 000 lignes, et ils variaient d'un facteur 5 pour la même requête.

### Conclusion pour la question 1

Avant de faire grossir quoi que ce soit, chercher les requêtes qui lisent beaucoup plus qu'elles ne renvoient (`pg_stat_statements`, puis `EXPLAIN (ANALYZE, BUFFERS)`). Sur 5 M de transactions, l'historique d'un compte sans index lisait toute la table (400 Mo) pour renvoyer 50 lignes ; un index composite `(account_id, created_at)` et une réécriture en `UNION ALL` le ramènent à 57 pages et 2 ms. Les clés étrangères non indexées sont l'autre gain rapide. À 65 M d'utilisateurs, c'est la différence entre lire des pages et lire la table, et le `OR` disparaît avec un grand livre en double entrée (une ligne par compte et par mouvement, un seul index).


---

## Mercredi 7 — Index partiels et couvrants, coût en écriture

### Méthode

- Index finaux sur `transactions` (`db/03-index.sql`) :

  ```sql
  CREATE INDEX idx_tx_from_cover ON transactions (from_account_id, created_at DESC)
      INCLUDE (amount_minor, status);
  CREATE INDEX idx_tx_to_created ON transactions (to_account_id, created_at);
  CREATE INDEX idx_tx_pending    ON transactions (created_at) WHERE status = 'PENDING';
  ```

  L'index couvrant remplace `idx_tx_from_created` de mardi.
- `EXPLAIN (ANALYZE, BUFFERS)`, cache chaud. Les temps sont l'`Execution Time` côté serveur.
- Débit en écriture : `pgbench -n -c 8 -j 4 -T 30 -f /bench/insert_tx.sql` (`INSERT` d'une ligne dans `transactions`, comptes aléatoires), `CHECKPOINT` avant chaque série, 2 exécutions par configuration.
- Le seed met ~1,5 % des transactions en `PENDING` (~75 000 lignes).

### Requêtes

**D. Job de relance : transactions en attente depuis plus d'une heure**

```sql
SELECT id, from_account_id, amount_minor
FROM transactions
WHERE status = 'PENDING' AND created_at < now() - interval '1 hour'
ORDER BY created_at
LIMIT 100;
```

**E. Historique d'un compte, avec seulement les colonnes d'un écran de liste**

```sql
SELECT created_at, amount_minor, status
FROM transactions
WHERE from_account_id = 42
ORDER BY created_at DESC
LIMIT 50;
```

### Résultats

Les plans complets sont en annexe de la version anglaise (`s1.md`).

**Index partiel (requête D)**

| # | Index | Nœuds principaux | Temps d'exécution | Buffers |
| --- | --- | --- | ---: | ---: |
| 9 | aucun | Parallel Seq Scan + tri top-N | 998,8 ms | 51 621 |
| 10 | `idx_tx_pending` (partiel) | Index Scan | 0,30 ms | 102 |

**~3 300× plus rapide, ~500× moins de pages.** Pour la ligne 10, un index complet sur `created_at` existait aussi : le planificateur a choisi le partiel.

| Index sur `created_at` | Taille |
| --- | ---: |
| Complet (5 M de lignes) | 107 Mo |
| Partiel (`WHERE status = 'PENDING'`) | 1,6 Mo (**65× plus petit**) |

**Index couvrant (requête E, compte chaud 42, après `VACUUM ANALYZE`)**

| # | Index | Nœuds principaux | Heap fetches | Temps d'exécution | Buffers |
| --- | --- | --- | ---: | ---: | ---: |
| 11 | `(from_account_id, created_at)` | Index Scan | — | 0,25 ms | 53 |
| 12 | couvrant, juste après `VACUUM FULL` | Index Only Scan | 50 | 0,39 ms | 56 |
| 13 | couvrant, après `VACUUM` | Index Only Scan | 0 | 0,39 ms | 22 |

**2,4× moins de pages, même temps** (tout était en cache).

**Coût en écriture (pgbench, insertions d'une ligne)**

| Configuration | Exécution 1 | Exécution 2 | Moyenne | Latence moy. |
| --- | ---: | ---: | ---: | ---: |
| Clé primaire + 3 index secondaires | 17 154 tps | 18 463 tps | **17 809 tps** | 0,45 ms |
| Clé primaire seule | 20 837 tps | 21 348 tps | **21 092 tps** | 0,38 ms |

**Les 3 index secondaires coûtent ~16 % du débit d'insertion.** L'écart entre deux exécutions identiques est de ~7 % : la différence est réelle, mais ne se lit pas au pourcent près. La liste exacte des index pendant les exécutions « avec index » n'a pas été relevée.

**Taille des index, état final**

| Index | Taille |
| --- | ---: |
| `idx_tx_from_cover` | 278 Mo |
| `idx_tx_to_created` | 150 Mo |
| `transactions_pkey` | 107 Mo |
| `idx_tx_pending` | 1,6 Mo |
| **Total** | **~537 Mo** (table : 403 Mo) |

### Observations

1. **Un index partiel ne stocke que les lignes qu'on interroge.** Un index sur `status` seul serait presque inutile (3 valeurs pour 5 M de lignes), mais la valeur rare est justement celle dont le job de relance a besoin. L'index partiel est 65× plus petit qu'un index complet : il coûte peu à maintenir et reste en mémoire.
2. **Un index couvrant supprime l'accès à la table, pas du temps, tant que tout est en cache.** 53 → 22 pages, mais 0,25 ms contre 0,39 ms, soit du bruit sous la milliseconde. Chaque page économisée est une lecture aléatoire évitée quand la table ne tient plus en mémoire : le gain grandit avec les données, le coût aussi.
3. **Un Index Only Scan a besoin d'une visibility map à jour.** Juste après `VACUUM FULL`, le plan affichait `Index Only Scan` mais faisait 50 accès à la table (un par ligne) et lisait 56 pages, plus que l'index simple. `VACUUM FULL` réécrit la table sans remplir la visibility map ; un `VACUUM` simple la remplit. Sur une table très active, le réglage de l'autovacuum influe directement sur les performances en lecture.
4. **22 pages pour 50 entrées d'index, c'est plus que prévu** (~4 : 3 niveaux + 1 feuille). Explication probable, non vérifiée : la visibility map d'une table de 51 621 pages occupe 2 pages, les lignes du compte 42 sont réparties dans les deux moitiés de la table, et chaque passage d'une page de la carte à l'autre compte comme un `hit`.
5. **`INCLUDE` n'est pas gratuit.** L'index couvrant fait 278 Mo contre 150 Mo pour la même clé sans colonnes incluses (+85 %). Les index pèsent désormais plus lourd que la table.
6. **Chaque insertion paie aussi les clés étrangères.** `pg_stat_statements` a compté 14,7 M d'appels (43 s au total) à `SELECT … FROM accounts … FOR KEY SHARE` : deux vérifications de clé étrangère par transaction insérée (5 M du seed + 2,3 M de pgbench, × 2).
7. **Un benchmark d'écriture modifie les données qu'il mesure.** Les quatre exécutions de pgbench ont inséré 2 333 965 lignes (5 M → 7,3 M). Elles ont été supprimées (`created_at >= '2026-10-06'`) et la table reconstruite avec `VACUUM FULL`, pour garder la base de 5 M lignes pour les mesures suivantes.
8. **`pg_stat_statements` doit être filtré avant d'être lu.** Le premier top 10 contenait le seed, les `CREATE INDEX` et le benchmark ; après `pg_stat_statements_reset()`, ce sont les requêtes de catalogue de pgAdmin qui arrivaient en tête. Les vraies requêtes applicatives étaient toutes rapides : 2,33 ms pour le job de relance (D), 1,95 ms pour les transactions sortantes d'un utilisateur (C). Deux vues comptent : le temps total (requêtes fréquentes et peu coûteuses) et le temps moyen (requêtes rares et lentes).

### Conclusion pour la question 1

Les index sont un compromis, pas un réflexe. Un index partiel fait passer un job d'une seconde à 0,3 ms pour 1,6 Mo ; un index couvrant supprime l'accès à la table pour la lecture la plus fréquente ; mais les trois index secondaires coûtent déjà ~16 % du débit d'insertion et pèsent plus lourd que la table. Sur une table aussi sollicitée en écriture que `transactions`, à 65 M d'utilisateurs, chaque index doit correspondre à une requête mesurée, et un index couvrant ne paie que si le vacuum tient la visibility map à jour.

---

## Jeudi 8 — Partitionnement par mois

### Méthode

- Copie partitionnée de `transactions` (`db/04-partition.sql`) : `transactions_p`, `PARTITION BY RANGE (created_at)`, **26 partitions mensuelles** (octobre 2024 → novembre 2026), clé primaire `(id, created_at)`. Sur chaque partition, les deux mêmes index par compte que mardi (et non l'index couvrant de mercredi) :

  ```sql
  CREATE INDEX ON transactions_p (from_account_id, created_at DESC);
  CREATE INDEX ON transactions_p (to_account_id, created_at DESC);
  ```

- Les mêmes 5 M de lignes dans les deux tables (`INSERT INTO transactions_p SELECT * FROM transactions`, puis `ANALYZE`). Données : 403 Mo dans les deux cas ; index : 505 Mo (partitionnée) contre 537 Mo (simple). Un mois complet représente ~208 000 lignes et 17 Mo.
- `EXPLAIN (ANALYZE, BUFFERS)`, chaque requête lancée deux fois, **deuxième exécution retenue** (cache chaud) sauf mention contraire. JIT désactivé (`SET jit = off`) pour la requête par période : à une première exécution, il prenait 62 ms sur 282.
- Référence table simple pour l'historique d'un compte : lignes 5 et 6 de mardi (même forme d'index).
- Archivage : le `DELETE` est mesuré avec `EXPLAIN ANALYZE` et le `DETACH` avec `\timing` dans psql, tous deux dans une transaction annulée.

### Requêtes

**F. Volume quotidien du mois dernier** (septembre 2026)

```sql
SELECT date_trunc('day', created_at) AS day, count(*), sum(amount_minor)
FROM transactions_p          -- ou transactions
WHERE created_at >= date_trunc('month', now()) - interval '1 month'
  AND created_at <  date_trunc('month', now())
GROUP BY 1
ORDER BY 1;
```

**G. Historique d'un compte** : requête B (`UNION ALL`) de mardi, avec les **deux** branches sur `transactions_p`.

**G'. Historique limité aux 12 derniers mois** : requête G avec une borne de date dans chaque branche, pour que l'élagage s'applique.

```sql
(SELECT * FROM transactions_p
 WHERE from_account_id = 900000 AND created_at >= now() - interval '1 year'
 ORDER BY created_at DESC LIMIT 50)
-- même borne dans la branche to_account_id
```

**H. Archivage de tout ce qui a plus de 18 mois**

```sql
BEGIN;
EXPLAIN ANALYZE DELETE FROM transactions WHERE created_at < now() - interval '18 months';
ROLLBACK;

BEGIN;
ALTER TABLE transactions_p DETACH PARTITION transactions_p_2024_10;
-- … idem pour 2024_11, 2024_12, 2025_01, 2025_02, 2025_03
SELECT count(*) FROM transactions_p;
ROLLBACK;
```

### Résultats

Les plans complets sont en annexe de la version anglaise (`s1.md`).

**Ce que le partitionnement accélère : les requêtes sur une période (F)**

| # | Table | Nœuds principaux | Planification | Temps d'exécution | Buffers |
| --- | --- | --- | ---: | ---: | ---: |
| 14 | `transactions` | Parallel Seq Scan (2 workers) + tri | 1,06 ms | 705,7 ms | 51 621 |
| 15 | `transactions_p` | 1 Seq Scan (`Subplans Removed: 25`) + tri | 2,0 ms | 159,6 ms | 2 145 |

**24× moins de pages, 4,4× plus rapide, avec 1 processus au lieu de 3.**

**Ce que le partitionnement ralentit : les requêtes par compte (G)**

| # | Table | Compte | Index de partition parcourus | Planification | Temps d'exécution | Buffers |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 5 | `transactions` (mardi) | 42 | — | — | 1,96 ms | 57 |
| 16 | `transactions_p` | 42 | 8 / 52 | 5,5 ms | 2,80 ms | 71 |
| 6 | `transactions` (mardi) | 900000 | — | — | 0,17 ms | 14 |
| 17 | `transactions_p` | 900000 | 52 / 52 | 1,87 ms | 0,71 ms | 158 |
| 17b | `transactions_p`, à froid (`read=148`) | 900000 | 52 / 52 | 5,9 ms | 21,0 ms | 158 |

Compte actif : **+25 % de pages, presque neutre**. Compte calme : **11× plus de pages**, 4× plus lent avec les pages en cache, 120× plus lent quand elles viennent du disque.

**Historique borné à 12 mois (G')**

| # | Requête | Compte | Index de partition parcourus | Planification | Temps d'exécution | Buffers | Lignes renvoyées |
| --- | --- | --- | ---: | ---: | ---: | ---: | ---: |
| 17 | G, sans borne | 900000 | 52 / 52 | 1,87 ms | 0,71 ms | 158 | 6 |
| 20 | G', 12 mois | 900000 | 28 / 52 (`Subplans Removed: 12` par branche) | 3,0 ms | 0,42 ms | 81 | **1** |
| 16 | G, sans borne | 42 | 8 / 52 | 5,5 ms | 2,80 ms | 71 | 50 |
| 21 | G', 12 mois | 42 | 8 / 52 | 5,8 ms | 2,51 ms | 71 | 50 |

Compte calme : **deux fois moins de pages, 1,7× plus rapide**. Compte actif : aucun changement.

**Archivage (H)**

| # | Table | Opération | Lignes archivées | Temps | Lignes mortes laissées |
| --- | --- | --- | ---: | ---: | ---: |
| 18 | `transactions` | `DELETE … WHERE created_at < now() - 18 mois` | 1 272 471 | 3 110,9 ms | 1 272 471 |
| 19 | `transactions_p` | 6 × `DETACH PARTITION` | 1 221 352 | 84,7 ms | 0 |

**37× plus rapide, et rien à nettoyer pour VACUUM.** Chaque `DETACH` a pris 11 à 17 ms (11,4 / 16,6 / 15,8 / 12,4 / 15,4 / 13,0 ms), pour ~200 000 lignes chacun.

La copie des 5 M de lignes dans la table partitionnée a pris 18,7 s (temps pgAdmin, indicatif).

### Observations

1. **L'élagage a eu lieu à l'exécution.** `Subplans Removed: 25` signifie que le planificateur a gardé les 26 partitions et que l'exécuteur en a écarté 25 au démarrage : les bornes utilisent `now()`, qui n'est pas une constante au moment de la planification. Avec des dates littérales, les partitions élaguées n'apparaîtraient pas du tout dans le plan.
2. **Les pages baissent bien plus que le temps quand tout est en cache.** 24× moins de pages, mais 4,4× plus rapide : sans lecture disque, le coût est du CPU, et le tri et l'agrégation des 208 000 lignes de septembre sont les mêmes sur les deux tables. Sur des données qui ne tiennent pas en mémoire, chaque page non lue est une lecture disque économisée. La table simple a aussi eu besoin de 3 processus, qu'un serveur chargé prendrait aux autres requêtes.
3. **Un `Append` ordonné s'arrête dès que le `LIMIT` est atteint.** Les partitions sont parcourues de la plus récente à la plus ancienne. Pour le compte actif, la branche `from` a trouvé ses 50 lignes en octobre et septembre 2026 : seuls 3 index de partition ont été parcourus, les 23 autres sont `never executed`.
4. **Le compte calme est le pire cas.** Il n'atteint jamais 50 lignes, donc les 26 × 2 index de partition sont tous sondés, à ~3 pages chacun : 158 pages au lieu de 14. Le partitionnement pénalise les comptes **peu** actifs, et le coût grandit avec le nombre de partitions, pas avec le volume de données.
5. **À froid ou à chaud, on voit où est le risque.** Les mêmes 158 pages ont pris 21 ms depuis le disque et 0,71 ms depuis le cache. Le nombre de pages est la mesure stable ; à 65 M d'utilisateurs, quand les données ne tiennent plus en mémoire, il se traduit en lectures aléatoires.
6. **`Merge Append` lit ses branches à la demande.** Pour le compte 42, la branche `to` s'est arrêtée après sa première ligne (juillet 2026) : elle était plus ancienne que les 50 lignes déjà fournies par la branche `from`.
7. **Le partitionnement ajoute un coût fixe de planification.** Environ 2 ms par requête avec 26 partitions, contre ~1 ms sur la table simple. La première requête après le `DETACH` / `ROLLBACK` a pris 19,8 ms à planifier ; explication probable, non vérifiée : la description des partitions a dû être rechargée dans le cache. Ce coût grandit avec le nombre de partitions, une raison de ne pas partitionner par jour.
8. **`DELETE` touche les lignes, `DETACH` touche le catalogue.** Le `DELETE` a lu toute la table (1,9 s de parcours séquentiel), puis écrit un `xmax` sur 1,27 M de lignes : après un `COMMIT`, ce seraient des lignes mortes (bloat, un `VACUUM` à payer sur la table et ses index, du WAL pour chaque ligne). Le `DETACH` transforme seulement une partition en table autonome, qu'on peut ensuite exporter vers un stockage froid puis supprimer : ~14 ms, quelle que soit la taille de la partition.
9. **La rétention doit suivre les bornes des partitions.** La date limite était le 8 avril 2025 : le `DETACH` a archivé 51 119 lignes de moins que le `DELETE`, parce que du 1er au 8 avril 2025 se trouve dans la partition d'avril, encore en partie dans la fenêtre. En production, la rétention se définit en mois entiers.
10. **Un `DETACH` simple prend un verrou `ACCESS EXCLUSIVE` sur la table parente** jusqu'à la fin de la transaction (d'après la documentation, non mesuré ici). En production : `DETACH PARTITION … CONCURRENTLY`, qui ne bloque ni les lectures ni les écritures, mais ne peut pas s'exécuter dans un bloc de transaction.
11. **Vérifier que chaque nœud du plan lit la table attendue.** La première mesure de l'historique avait encore la seconde branche du `UNION ALL` sur `transactions` : le plan mélangeait les deux tables (`Index Scan Backward using idx_tx_to_created on transactions`). Elle a été écartée et refaite.

12. **Une borne de date aide exactement les comptes que le partitionnement pénalise.** Sur 12 mois, 12 des 26 partitions sont élaguées dans chaque branche, et les pages du compte calme baissent en proportion des partitions conservées (28 / 52 × 158 ≈ 85 ; mesuré : 81). Le compte actif ne gagne rien : les partitions élaguées étaient déjà `never executed`. Une fenêtre plus courte (3 mois par exemple) rapprocherait le compte calme de la table simple ; non mesuré.
13. **Une borne de date change le résultat.** Le compte 900000 renvoie 1 ligne au lieu de 6 : les 5 autres ont plus d'un an. Borner l'historique est un choix produit, pas une optimisation transparente : l'écran affiche « 12 derniers mois », et « voir plus » interroge la fenêtre précédente avec un curseur sur `created_at`.
14. **Le temps de planification est bruité à cette échelle.** Entre 1,9 et 7,1 ms selon les exécutions d'une même requête, toutes les pages étant en cache. Des écarts de quelques millisecondes de planification ne sont pas significatifs ici sans plus d'exécutions.

### Conclusion pour la question 1

À 65 M d'utilisateurs, `transactions` grossit d'environ 66 Go par mois : la question est de vivre avec le flux, pas de stocker le stock. Le partitionnement par mois rend bon marché les deux grosses opérations liées au temps : un mois de reporting lit 24× moins de pages, et l'archivage de 18 mois passe d'un `DELETE` de 3,1 s qui laisse 1,27 M de lignes mortes à un `DETACH` de 85 ms. Le prix se paie sur les lectures par compte : un compte calme sonde toutes les partitions (158 pages au lieu de 14), plus un coût de planification qui grandit avec le nombre de partitions. La parade consiste à borner l'historique d'un compte dans le temps et à paginer par curseur sur `created_at`, pour que l'élagage s'applique : avec une fenêtre de 12 mois, le compte calme lit déjà deux fois moins de pages (81 au lieu de 158). C'est préférable à un partitionnement par hash de compte, ce qui ramènerait l'archivage à un `DELETE`.
