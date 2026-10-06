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
  CREATE INDEX idx_from_account  ON transactions (from_account_id, created_at);
  CREATE INDEX idx_to_account    ON transactions (to_account_id,   created_at);
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
