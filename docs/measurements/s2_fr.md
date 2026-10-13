# Mesures — semaine 2

## Environnement

| | |
| --- | --- |
| Machine | MacBook, Apple M2 Pro (10 cœurs), 16 Go de RAM |
| JVM | Java 21 |
| Module | `engine/` (Java pur, aucune dépendance de production) |
| Solde initial | 5 000 par compte |

---

## Lundi 12 — `NaiveEngine`

### Prédiction (avant mesure)

1. **Signe de l'écart :** positif, parce qu'on perd potentiellement plus de débits que de crédits.
2. **Comparé à S1 (+5,9 à +8,2 %) :** plus grand, parce que le nombre de virements concurrents (et donc de débits perdus) augmente.
3. **10 ou 1 000 comptes :** l'écart sera plus grand avec 10 comptes, parce que la probabilité de collision diminue quand le nombre de comptes augmente.

### Méthode

`EngineLab --engine=naive --threads=1000 --transfers=1000 --virtual --work=20 --hot=50 --audit`, 5 lancements avec `--accounts=10`, puis 5 avec `--accounts=1000`.

⚠️ Ce ne sont pas les conditions prévues (threads de plateforme, sans travail, sans compte chaud) :

- `--work=20` n'a eu **aucun effet** : `NaiveEngine.transfer` n'appelle pas `Work.simulate()`.
- `--hot=50` envoie la moitié des virements sur le compte 0, **quel que soit le nombre de comptes**. La comparaison entre 10 et 1 000 comptes est donc faussée.

### Résultats

Écart = `after - before`, en pourcentage du total initial.

| Comptes | Lancement | Durée | Virements acceptés | Refusés | Écart | Écart (%) | Audit min / max |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 10 | 1 | 102 ms | 895 933 | 4 725 | +2 144 014 | +4 288 % | 40 458 / 2 185 021 |
| 10 | 2 | 105 ms | 894 032 | 6 192 | +1 290 794 | +2 582 % | 53 312 / 1 304 417 |
| 10 | 3 | 121 ms | 893 469 | 6 873 | +1 606 478 | +3 213 % | 53 974 / 1 762 961 |
| 10 | 4 | 109 ms | 894 083 | 5 678 | +1 154 146 | +2 308 % | 50 332 / 1 247 887 |
| 10 | 5 | 124 ms | 892 782 | 6 937 | +1 604 272 | +3 209 % | 68 984 / 1 643 016 |
| 1 000 | 1 | 134 ms | 927 463 | 71 500 | +1 452 037 | +29,0 % | 4 986 091 / 6 525 481 |
| 1 000 | 2 | 111 ms | 918 533 | 80 416 | +931 826 | +18,6 % | 4 944 222 / 5 929 426 |
| 1 000 | 3 | 127 ms | 925 693 | 73 273 | +1 517 391 | +30,3 % | 4 982 997 / 6 488 277 |
| 1 000 | 4 | 131 ms | 924 069 | 74 942 | +1 242 915 | +24,9 % | 5 001 047 / 6 227 055 |
| 1 000 | 5 | 118 ms | 934 353 | 64 581 | +2 403 862 | +48,1 % | 4 978 740 / 7 398 676 |

**Médianes :** 10 comptes : +1 604 272 (+3 209 %) ; 1 000 comptes : +1 452 037 (+29,0 %).

### Constats

- `BROKEN` 10 fois sur 10, et l'écart est **toujours positif**.
- L'écart est des centaines de fois plus grand qu'en S1, en pourcentage.
- En **valeur absolue**, l'écart est du même ordre avec 10 et 1 000 comptes (environ 0,9 à 2,4 M). C'est le pourcentage qui change, parce que le total initial est 100 fois plus grand.
- Pendant les virements, la somme lue est passée **sous** le total initial (minimum 40 458 pour 50 000). Ce n'est pas forcément un crédit perdu : `total()` lit les comptes un par un, sans verrou, et peut tomber entre le débit et le crédit d'un virement en cours.

### Conditions de base (sans `--virtual`, `--work` ni `--hot`)

`EngineLab --engine=naive --threads=1000 --transfers=1000`, 5 lancements par nombre de comptes. Seule la ligne `invariant:` a été gardée.

| Comptes | Lancement | Écart | Écart (%) |
| ---: | ---: | ---: | ---: |
| 10 | 1 | +687 375 | +1 375 % |
| 10 | 2 | +888 715 | +1 777 % |
| 10 | 3 | +243 344 | +487 % |
| 10 | 4 | +574 183 | +1 148 % |
| 10 | 5 | +621 542 | +1 243 % |
| 1 000 | 1 | +1 682 | +0,03 % |
| 1 000 | 2 | +69 662 | +1,39 % |
| 1 000 | 3 | +35 062 | +0,70 % |
| 1 000 | 4 | +31 405 | +0,63 % |
| 1 000 | 5 | +34 033 | +0,68 % |

**Médianes :** 10 comptes : +621 542 (+1 243 %) ; 1 000 comptes : +34 033 (+0,68 %), soit environ 18 fois moins en valeur absolue.

### Comparaison avec S1, à conditions comparables

Le pourcentage ne se compare pas : en S1, 10 comptes à 100 000 et environ 560 virements validés ; ici, 10 comptes à 5 000 et environ 900 000 virements. On compare donc **l'écart par virement** (montant moyen d'un virement : environ 500).

| | Virements validés | Écart médian | Écart par virement | Part des virements qui perdent un débit (≈ écart / 500) |
| --- | ---: | ---: | ---: | ---: |
| S1, `NaiveTransfer` (PostgreSQL, 16 threads) | ≈ 560 | +63 769 | ≈ 114 | ≈ 23 % |
| S2, `NaiveEngine` (mémoire, 1 000 threads, 10 comptes) | ≈ 900 000 | +621 542 | ≈ 0,7 | ≈ 0,14 % |

### Expérience : sans le test de solde

**Hypothèse :** les débits se perdent plus souvent parce que leur fenêtre va du `if (balances[from] < amount)` jusqu'à l'écriture (le JIT peut réutiliser la valeur lue dans le test). Sans le `if`, débit et crédit ont la même fenêtre : le signe de l'écart devrait devenir variable.

**Méthode :** `if` commenté dans `NaiveEngine`, `--threads=1000 --transfers=1000 --audit`, 5 lancements par nombre de comptes. Le `if` a été remis ensuite.

| Comptes | Écarts (5 lancements) | Signe |
| ---: | --- | --- |
| 1 000 | −10 401 ; +19 209 ; +7 653 ; −13 261 ; −48 604 | **variable** (3 négatifs, 2 positifs) |
| 10 | +1 734 453 ; +289 806 ; +264 361 ; +484 489 ; +580 140 | **toujours positif** |

**Constats :**

- Avec 1 000 comptes, l'hypothèse tient : sans le test, le signe devient variable.
- Avec 10 comptes, l'écart reste positif : le test de solde n'explique pas toute l'asymétrie quand la contention est extrême. **Question ouverte.** Prochaine expérience : inverser l'ordre (crédit puis débit). Si l'écart devient négatif, c'est l'ordre des écritures qui compte.
- Sans le test, les soldes passent sous zéro : la somme lue pendant les virements est descendue jusqu'à −293 910.

### Bilan de la prédiction

| Prédiction | Verdict |
| --- | --- |
| 1. Écart positif | ✅ Positif dans les 15 lancements |
| 2. Plus grand qu'en S1 | ✅ en pourcentage et en valeur absolue, ❌ par virement : un virement en mémoire a environ 160 fois moins de chances de perdre un débit |
| 3. Plus grand avec 10 comptes | ✅ environ 18 fois plus grand qu'avec 1 000 comptes, en valeur absolue |

## Mardi 13 — `GlobalLockEngine`

Tout le corps de `transfer` s'exécute dans `synchronized (lock)`, et `total()` aussi. `Work.simulate()` n'est appelé qu'après un virement réussi : un virement refusé (fonds insuffisants) ne garde presque pas le verrou.

### Prédiction (avant mesure)

| Comptes | `--work` | `--transfers` | Durée prédite |
| ---: | ---: | ---: | --- |
| 10 | 0 | 1 000 | non écrite (à faire avant de mesurer) |
| 1 000 | 0 | 1 000 | non écrite |
| 10 | 20 µs | 100 | 2 s |
| 1 000 | 20 µs | 100 | 2,5 s |

Plafond théorique d'un verrou unique : 1 / durée de la section critique = 1 / 20 µs = **50 000 virements/s**, quel que soit le nombre de threads ou de cœurs.

### Méthode

`EngineLab --engine=globalLock --threads=1000 --audit`, avec `--accounts` = 10 ou 1 000, `--work` = 0 ou 20, `--transfers` = 100 ou 1 000 (le guide prévoyait `--transfers=100` avec `--work=20` ; les lancements à 1 000 ont été ajoutés pour comparer). 3 lancements chacun, médiane gardée.

⚠️ `--audit` était activé à chaque lancement : l'auditeur prend le verrou toutes les millisecondes. L'impact est faible, mais tous les moteurs doivent être mesurés dans les mêmes conditions cette semaine.

### Résultats (médianes de 3 lancements)

| Comptes | `--work` | `--transfers` | Durée | Acceptés | Refusés | Acceptés/s | Temps par virement accepté | Dispersion (min–max) |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 10 | 0 | 1 000 | 180 ms | 822 812 | 77 135 | 4,57 M | ≈ 0,22 µs | 138–183 ms |
| 1 000 | 0 | 1 000 | 194 ms | 907 263 | 91 727 | 4,68 M | ≈ 0,21 µs | 160–203 ms |
| 10 | 20 µs | 100 | 2 515 ms | 82 269 | 7 829 | 32 711 | ≈ 30,6 µs | 2 490–2 541 ms |
| 1 000 | 20 µs | 100 | 2 980 ms | 93 335 | 6 569 | 31 320 | ≈ 31,9 µs | 2 758–3 078 ms |
| 10 | 20 µs | 1 000 | 23 863 ms | 823 946 | 76 387 | 34 528 | ≈ 29,0 µs | 23 859–24 998 ms |
| 1 000 | 20 µs | 1 000 | 26 645 ms | 909 472 | 89 577 | 34 132 | ≈ 29,3 µs | 26 442–27 410 ms |

Invariant `OK` dans les 18 lancements ; audit `min = max` (50 000 ou 5 000 000) à chaque lancement.

### Vérifier l'instrument : combien dure vraiment `parkNanos(20 µs)` ?

**Prédiction :** environ 30 µs.

**Méthode :** 10 000 × `LockSupport.parkNanos(20_000)` dans un seul thread, sans verrou.

**Résultat :** **28,1 µs par `park`**, soit 8 µs de plus que demandé.

| | µs |
| --- | ---: |
| Un `parkNanos(20_000)` seul | 28,1 |
| Un virement accepté sous le verrou global (`--transfers=1000`) | ≈ 29 |
| **Coût de la passation du verrou** | **≈ 1** |

### Constats

- **Le nombre de comptes ne change rien au débit** (≈ 33 000–34 500/s avec 10 ou 1 000 comptes). Chaque virement fait la queue sur le même verrou, quels que soient les comptes touchés. Les lancements à 1 000 comptes durent plus longtemps seulement parce qu'ils font plus de travail utile : presque aucun `from == to` sauté (≈ 10 % avec 10 comptes), donc ≈ 909 000 virements acceptés au lieu de ≈ 823 000.
- **Mesuré : ≈ 34 000/s, pour un plafond calculé de 50 000/s.** La formule tient ; c'est l'entrée qui était fausse : sur ce Mac, `--work=20` coûte en réalité ≈ 28 µs. Plafond réel : 1 / 28,1 µs ≈ 35 600/s, donc le verrou global atteint **≈ 97 % de son plafond réel**.
- **La passation du verrou coûte ≈ 1 µs**, même avec 1 000 threads en attente. Presque toute la section critique est le travail fait sous le verrou.
- **Sans travail, le verrou lui-même coûte ≈ 0,2 µs par virement** (≈ 4,6 M/s). Des lancements de 140–200 ms sont trop courts pour être précis (montée en charge du JIT, 30 % d'écart) : un ordre de grandeur seulement.
- **La somme lue pendant les virements est toujours exacte**, parce que `total()` prend le même verrou que `transfer` (à comparer avec `NaiveEngine` : minimum 40 458 pour 50 000).
- **Biais connu pour toute la semaine :** `--work=20` vaut ≈ 28 µs sur cette machine. Il s'applique à tous les moteurs, donc les comparaisons restent valables, mais les plafonds se calculent avec 28 µs.
- Les lancements longs sont plus stables : ≈ 4–5 % d'écart avec `--transfers=1000`, jusqu'à 11 % avec `--transfers=100`.

### Bilan de la prédiction

| Prédiction | Verdict |
| --- | --- |
| 10 comptes, `--work=20` : 2 s | ❌ 2,5 s (+26 %) : la prédiction comptait 20 µs par virement ; le coût réel est ≈ 30 µs |
| 1 000 comptes, `--work=20` : 2,5 s (plus long qu'avec 10 comptes) | ✅ sur le sens, ❌ sur la valeur : 3,0 s (+19 %) |
| `parkNanos(20 µs)` dure ≈ 30 µs | ✅ 28,1 µs |

**Référence pour la semaine :** un verrou plus fin n'a d'intérêt que s'il dépasse ≈ 34 000/s avec `--work=20` et ≈ 4,6 M/s sans travail.
