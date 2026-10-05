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
