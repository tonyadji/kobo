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

## Mercredi 14 — Un verrou par compte, et le deadlock

`UnorderedLockEngine` et `OrderedLockEngine` : `long[] balances` et un `ReentrantLock` par compte (`ReentrantLock[] locks`, construit avec `Arrays.setAll`). Le moteur non ordonné verrouille `from`, puis `to`. Le moteur ordonné verrouille `min(from, to)`, puis `max(from, to)` : un seul endroit décide de l'ordre (règle de l'ADR-001). `total()` ne prend aucun verrou : ce n'est pas un instantané cohérent pendant les virements (à mesurer avec `--audit` jeudi).

### Bugs trouvés avant de mesurer

Trouvés en relecture de code, avant tout lancement mesuré. Chacun aurait rendu les mesures sans valeur.

| # | Bug | Effet | Correction |
| --- | --- | --- | --- |
| 1 | `synchronized (balances[from])` sur un `Long[]` | Verrouille la *valeur*, pas le compte. `Long` est immuable : `balances[from] -= amount` met un **nouvel** objet dans la case, donc deux threads peuvent « tenir le compte 1 » en même temps. Au départ, `Arrays.fill` mettait **le même** `Long` dans toutes les cases (une seule conversion), et `Long.valueOf` partage les objets entre −128 et 127. | Séparer données et verrous : `long[]` + un objet verrou par compte, jamais remplacé |
| 2 | Ordre fondé sur les soldes (`balances[from] < balances[to]`) | L'ordre change à chaque virement et il est lu sans verrou : deux threads peuvent voir deux ordres différents pour la même paire | Ordre par identifiant de compte |
| 3 | `total()` verrouillait le tableau (`synchronized (balances)`) | Aucun virement ne prend ce verrou : aucune exclusion | Aucun verrou, documenté (voir plus haut) |
| 4 | `Arrays.fill(locks, new ReentrantLock())` | `new` n'est évalué qu'une fois : **un seul** verrou partagé par tous les comptes, donc un verrou global caché (réentrant, donc jamais de deadlock) | `Arrays.setAll(locks, i -> new ReentrantLock())` |

Lancements avec les versions boguées (exclus de la comparaison) :

- Bug 2 (ancien `OrderedLockEngine`, `synchronized` sur `Long`) : **deadlock**. Prédiction : invariant cassé ou deadlock.
- Bug 4 (`UnorderedLockEngine` avec un seul verrou partagé, `--accounts=10 --work=20`) : `STUCK after 30 s, ok=808,577, deadlocked threads=0`. **Pas un deadlock** : un verrou global, simplement trop lent pour le chien de garde de 30 s (≈ 37 µs par virement accepté, contre ≈ 29 µs avec `synchronized` ; un seul lancement, coupé avant la fin : une observation, pas une mesure).

**Leçon :** `STUCK` est un délai dépassé, pas un diagnostic. Le diagnostic, c'est `deadlocked threads` (`ThreadMXBean.findDeadlockedThreads()`) ou `jstack`.

### Prédiction (avant mesure)

| Lancement | Prédiction | Raisonnement |
| --- | --- | --- |
| `globalLock` à nouveau, sans `--audit` | non écrite (lancement de contrôle) | — |
| `unorderedLock`, 10 comptes, `--work=20` | ≈ 11 virements acceptés avant le deadlock | 45 paires, 1 000 threads : une collision en sens inverse arrive presque tout de suite (paradoxe des anniversaires) |
| `unorderedLock`, 1 000 comptes, `--work=20` | ≈ 1 000 | — |
| `orderedLock`, 10 comptes, `--work=0` | 22 M/s | 0,22 µs par virement sous le verrou global, avec plusieurs virements en parallèle |
| `orderedLock`, 1 000 comptes, `--work=0` | 23 M/s | idem |
| `orderedLock`, 10 comptes, `--work=20` | ≈ 175 000/s | plafond 5 paires en parallèle / 28 µs ≈ 180 000/s, atteint à 97 % comme le verrou global |
| `orderedLock`, 1 000 comptes, `--work=20` | ≈ 17,5 M/s | plafond 500 paires en parallèle / 28 µs ≈ 18 M/s |
| `orderedLock`, 100 000 comptes, `--work=20` | non écrite | — |
| `naive`, 100 000 comptes, `--work=20` | non écrite | — |

### Méthode

`EngineLab --threads=1000 --transfers=1000`, threads classiques, **sans `--audit`** (contrairement à mardi), 3 lancements par configuration, médiane gardée. Le verrou global a été remesuré dans ces conditions pour que tous les moteurs soient comparés sur la même base.

### Résultats : verrou global à nouveau, sans `--audit` (médianes de 3 lancements)

| Comptes | `--work` | Durée | Acceptés | Refusés | Acceptés/s | Temps par virement accepté | Dispersion (min–max) |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 10 | 0 | 223 ms | 823 907 | 75 861 | ≈ 3,7 M | — | 155–241 ms |
| 1 000 | 0 | 171 ms | 908 187 | 90 795 | ≈ 5,3 M | — | 155–180 ms |
| 10 000 | 0 | 254 ms | 934 253 | 65 638 | ≈ 3,7 M | — | 173–283 ms |
| 10 | 20 µs | 23 785 ms | 824 367 | 75 936 | **34 659** | ≈ 28,9 µs | 23 721–23 870 ms |
| 1 000 | 20 µs | 26 326 ms | 907 964 | 91 083 | **34 489** | ≈ 29,0 µs | 26 295–26 536 ms |
| 10 000 | 20 µs | 27 147 ms | 933 931 | 65 952 | 34 402 | ≈ 29,1 µs | 26 964–27 514 ms |

La série à 10 000 comptes a été lancée par erreur à la place de 1 000, puis 1 000 a été lancé aussi ; elle est gardée comme point supplémentaire. Lancé aussi avec `--transfers=100` (médianes) : 10 comptes : 47 ms (≈ 1,7 M/s) et 2 465 ms (33 454/s) ; 1 000 comptes : 40 ms (≈ 2,3 M/s) et 2 930 ms (31 872/s). Invariant `OK` dans les 27 lancements.

### Résultats : deadlock (`unorderedLock`, 10 comptes, `--work=20`)

`unorderedLock: STUCK after 30 s, ok=13, deadlocked threads=4`

`jstack` pendant les 30 s (cycle en annexe) : un **cycle de 2 threads** (le thread 9 tient `…740` et attend `…7a0` ; le thread 15 tient `…7a0` et attend `…740`), avec les threads 1 et 8 bloqués derrière (1 → 8 → 9 ⇄ 15). `deadlocked threads=4` compte les threads du cycle **et** ceux qui sont bloqués dessus. Un vidage pris quelques secondes plus tard, lors d'un autre lancement, montrait **1 003 threads en attente d'un `ReentrantLock`, répartis sur les 10 verrous** (de 88 à 118 par verrou) : un cycle de 2 threads a gelé toute la banque, parce que chaque thread en attente tenait déjà un verrou.

JUnit avec `unorderedLock` ajouté au `@ValueSource` : résultat non noté.

### Résultats : `orderedLock` (médianes de 3 lancements)

| Comptes | `--work` | Durée | Acceptés | Refusés | Acceptés/s | Verrou global (acceptés/s) | Écart | Dispersion (min–max) |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 10 | 0 | 337 ms | 822 840 | 77 023 | ≈ 2,4 M | ≈ 3,7 M | −35 % | 329–355 ms |
| 1 000 | 0 | 197 ms | 908 593 | 90 373 | ≈ 4,6 M | ≈ 5,3 M | −13 % (bruit) | 181–202 ms |
| 10 | 20 µs | 29 349 ms | 823 068 | 76 829 | **28 044** | 34 659 | **−19 %** | 29 262–29 768 ms |
| 1 000 | 20 µs | 20 688 ms | 906 830 | 92 205 | **43 833** | 34 489 | **+27 %** | 20 687–20 844 ms |
| 100 000 | 20 µs | 13 463 ms | 991 324 | 8 660 | **73 633** | — | — | 13 354–13 480 ms |

Invariant `OK` dans les 15 lancements.

### Expérience : où est le plafond ? (`naive`, 100 000 comptes, `--work=20`)

**Hypothèse :** si le faible gain des verrous par compte vient de la contention, il devrait presque disparaître avec 100 000 comptes. Le lancement `orderedLock` à 100 000 comptes dit le contraire : seulement ≈ 2 virements en cours. Pour savoir si ce sont les verrous qui limitent, on mesure un moteur **sans aucun verrou** dans les mêmes conditions.

| Lancement | Durée | Acceptés | Acceptés/s | Écart |
| ---: | ---: | ---: | ---: | ---: |
| 1 | 12 726 ms | 991 651 | 77 923 | −109 |
| 2 | 12 435 ms | 991 546 | 79 738 | −639 |
| 3 | 12 477 ms | 991 418 | 79 459 | +116 |

**Médiane : 79 459/s**, seulement 8 % de plus que `orderedLock` (73 633/s). Invariant `BROKEN` dans les 3 lancements, avec un signe aléatoire.

### Lire le débit avec la loi de Little

Virements en cours en moyenne = débit × durée d'un virement (≈ 28,1 µs de travail) :

| Moteur (`--work=20`) | Acceptés/s | En cours | Part du plafond du banc (≈ 80 000/s) |
| --- | ---: | ---: | ---: |
| Verrou global | 34 500 | ≈ 1,0 | ≈ 43 % |
| Ordonné, 10 comptes | 28 044 | ≈ 0,8 | ≈ 35 % |
| Ordonné, 1 000 comptes | 43 833 | ≈ 1,2 | ≈ 55 % |
| Ordonné, 100 000 comptes | 73 633 | ≈ 2,1 | ≈ 93 % |
| Naïf (sans verrou), 100 000 comptes | 79 459 | ≈ 2,2 | 100 % |

### Constats

- **L'ordre supprime les deadlocks, pas l'attente.** À 10 comptes, le moteur ordonné est **19 % plus lent qu'un seul verrou** (≈ 0,8 virement en cours). Un thread qui tient le compte 3 en attendant le compte 7 bloque tous ceux qui veulent le compte 3 (l'attente en tenant un verrou est toujours là), et chaque virement demande deux passations de verrou au lieu d'une (≈ 35,7 µs par virement accepté, contre ≈ 29 µs).
- **Les verrous par compte ne sont rentables que si la contention est faible :** 28 000 → 44 000 → 74 000/s pour 10 → 1 000 → 100 000 comptes. Avec 1 000 threads et 1 000 comptes, 2 000 verrous sont demandés pour 1 000 comptes : presque chaque compte est disputé.
- **Sans travail, le verrou global gagne** (3,7 M contre 2,4 M/s à 10 comptes) : quand il n'y a presque rien à faire sous le verrou, prendre deux verrous coûte plus que ce que le parallélisme rapporte, et le parallélisme est limité par les 10 cœurs, pas par le nombre de comptes. Lancements de 200–350 ms : ordre de grandeur seulement.
- **Le banc a son propre plafond : ≈ 80 000/s à `--work=20` avec des threads classiques**, quel que soit le moteur (≈ 2 threads réellement actifs en même temps). Sans contention, les verrous par compte ne coûtent que ≈ 7 %. Hypothèse : le plafond vient des réveils de threads par le système (chaque `parkNanos` endort un vrai thread du système) ; à vérifier avec `--virtual` vendredi. Toutes les comparaisons de la semaine doivent se lire par rapport à ce plafond, pas aux 500 paires en parallèle théoriques.
- **Une course rare reste un bug certain :** avec 100 000 comptes, le moteur naïf a cassé l'invariant dans les 3 lancements, de quelques centaines sur 500 M.
- **La JVM ne détecte rien :** en S1, PostgreSQL a détecté ≈ 290 deadlocks et tué une transaction à chaque fois (150 s au lieu de l'infini). Ici, 13 virements sur 900 000 sont passés, puis le programme aurait attendu pour toujours.
- **Comparer les virements acceptés par seconde, pas les durées.** Les virements refusés et les paires `from == to` sautées (≈ 10 % avec 10 comptes) n'appellent pas `Work.simulate()` : les lancements avec plus de comptes durent plus longtemps seulement parce qu'ils acceptent plus de virements (≈ 29 µs par virement accepté dans toutes les configurations du verrou global).
- Sans `--audit`, le verrou global donne le même débit que mardi à `--work=20` (≈ 34 500/s) : l'audit ne coûtait rien de mesurable.

### Bilan de la prédiction

| Prédiction | Verdict |
| --- | --- |
| Non ordonné, 10 comptes : ≈ 11 virements avant le deadlock | ✅ 13 |
| Non ordonné, 1 000 comptes : ≈ 1 000 | — non mesuré |
| Ordonné, 10 comptes, `--work=0` : 22 M/s | ❌ ≈ 2,4 M/s (9 fois moins), et plus lent que le verrou global : sans travail, le lancement est limité par le processeur et chaque virement prend deux verrous |
| Ordonné, 1 000 comptes, `--work=0` : 23 M/s | ❌ ≈ 4,6 M/s (5 fois moins) |
| Ordonné, 10 comptes, `--work=20` : ≈ 175 000/s | ❌ 28 044/s (6 fois moins), et **plus lent** que le verrou global : le plafond supposait que les paires ne se recouvrent jamais |
| Ordonné, 1 000 comptes, `--work=20` : ≈ 17,5 M/s | ❌ 43 833/s (400 fois moins) : la contention, plus le plafond du banc |
| Ordonné et naïf, 100 000 comptes | non écrites |

### Questions ouvertes

1. D'où vient le plafond du banc (≈ 80 000/s) avec des threads classiques ? À vérifier avec `--virtual` (vendredi).
2. Une passation de `ReentrantLock` disputé coûte-t-elle vraiment plus cher qu'une passation de `synchronized` sur cette machine (≈ 37 contre ≈ 29 µs, un seul lancement coupé) ? Un moteur à `ReentrantLock` global y répondrait.
3. `unorderedLock` à 1 000 comptes : combien de virements avant le deadlock ?

**Référence pour la suite de la semaine** (`--transfers=1000`, sans `--audit`) : verrou global ≈ 34 500/s avec `--work=20` et quelques M/s sans travail ; plafond du banc ≈ 80 000/s avec `--work=20` et des threads classiques.

---

## Jeudi 15 — `AtomicEngine` (CAS, sans verrou)

Mesuré le samedi 17 (la semaine a environ deux jours de retard). `AtomicEngine` : `AtomicLongArray balances`. Débit : une boucle `while (true)` qui lit le solde, lève `InsufficientFundsException` s'il est trop bas, sinon tente `compareAndSet(from, current, current - amount)` ; en cas d'échec, elle incrémente `casFailures` (un `LongAdder` statique) et recommence. Puis `Work.simulate()`, puis le crédit par `addAndGet(to, amount)` (pas de règle métier, il ne peut pas échouer). `total()` lit les comptes un par un, sans verrou. `casFailures()` est une méthode `default` de `TransferEngine` (0 pour les autres moteurs), affichée par `EngineLab`.

JUnit : `atomicEngine` ajouté au `@ValueSource`, **vert**.

### Bugs trouvés avant de mesurer

| # | Bug | Effet | Correction |
| --- | --- | --- | --- |
| 1 | Lecture et test du solde **avant** la boucle `while` | Après un CAS raté, la boucle réessaie avec la même valeur `expected` périmée et ne relit jamais le solde : elle tourne **à l'infini** à la première collision (sauf si le solde revient à l'ancienne valeur, un ABA). Même avec une relecture, un test hors de la boucle laisse un rejeu débiter un solde que personne n'a vérifié (→ solde négatif). | Lecture, test et CAS dans la boucle, sur la **même** valeur lue : un CAS réussi prouve que le test est encore vrai (même idée que `WHERE balance_minor >= ?` en S1) |
| 2 | Moteur non déclaré dans `Engines.create` | `Unknown engine` | `case "atomicEngine"` |
| 3 | `casFailures` inséré au milieu des arguments du `printf` | `rejected` aurait affiché les échecs de CAS, `ok/s` les refus et `casFailures` le débit | `casFailures` en dernier argument |

### Prédiction (avant mesure)

Hypothèse : un CAS coûte 0 (aucune idée de son coût ; à déduire des résultats). Échecs de CAS et audit non prédits.

| Lancement | Prédiction | Raisonnement |
| --- | --- | --- |
| 10 comptes, `--work=0` | 500 M/s | 0,2 µs par virement, 10 en parallèle |
| 10 comptes, `--work=20` | 357 000/s | 28 µs par virement, 10 en parallèle |
| 1 000 comptes, `--work=0` | 50 G/s | 100 fois l'estimation à 10 comptes |
| 1 000 comptes, `--work=20` | 35,7 M/s | 100 fois l'estimation à 10 comptes |

### Méthode

`EngineLab --threads=1000 --transfers=1000`, threads classiques, sans `--audit`, 3 lancements par configuration, médiane de chaque colonne. Puis un lancement par moteur avec `--audit --accounts=1000 --work=20`.

### Résultats (médianes de 3 lancements)

| Comptes | `--work` | Durée | Acceptés | Refusés | Acceptés/s | Échecs de CAS | Échecs par virement accepté | Écart (min–max) |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| 10 | 0 | 175 ms | 822 769 | 77 541 (8,6 %) | ≈ 4,7 M | 71 472 | **8,7 %** | 160–323 ms |
| 1 000 | 0 | 172 ms | 909 435 | 89 592 (9,0 %) | ≈ 5,3 M | 2 480 | **0,27 %** | 170–174 ms |
| 10 | 20 µs | 1 225 ms | 112 675 | 787 284 (**87 %**) | 91 889 | 491 | 0,44 % | 1 158–1 278 ms |
| 1 000 | 20 µs | 11 399 ms | 899 123 | 99 862 (10 %) | **78 915** | 127 | 0,014 % | 11 354–11 417 ms |

Invariant `OK` sur les 12 lancements. À 10 comptes sans travail, les échecs de CAS varient beaucoup d'un lancement à l'autre (67 981 à 152 060) : lancements de ≈ 170 ms, ordre de grandeur seulement.

Comparaison avec mercredi (acceptés/s, sans `--audit`) :

| Comptes | `--work` | Verrou global | Ordonné | Atomique |
| ---: | ---: | ---: | ---: | ---: |
| 10 | 0 | ≈ 3,7 M | ≈ 2,4 M | ≈ 4,7 M |
| 1 000 | 0 | ≈ 5,3 M | ≈ 4,6 M | ≈ 5,3 M |
| 10 | 20 µs | 34 659 | 28 044 | 91 889 (mais 87 % de refus) |
| 1 000 | 20 µs | 34 489 | 43 833 | **78 915** (≈ le plafond du banc, 80 000/s) |

### Audit (`--audit --accounts=1000 --work=20`, un lancement par moteur)

Le banc lit `total()` toutes les millisecondes pendant les virements. Valeur attendue : 5 000 000.

| Moteur | min | max | Acceptés/s |
| --- | ---: | ---: | ---: |
| `globalLock` | 5 000 000 | 5 000 000 | 34 353 |
| `orderedLock` | 4 998 575 | **5 000 985** | 43 621 |
| `atomicEngine` | **4 496 159** | 4 997 389 | 78 612 |

Invariant `OK` après chaque lancement (tout l'argent est là **à la fin**). L'audit ne change pas le débit (mêmes valeurs que sans `--audit`).

### Pourquoi l'audit donne ces résultats

**`atomicEngine` : l'argent est en vol.** Quand `total()` s'exécute, certains virements ont été débités mais pas encore crédités. Environ 1 000 threads × 500 en moyenne ≈ 500 000, et l'audit a mesuré jusqu'à 503 841 manquants. Le max reste sous 5 000 000 : il n'y a jamais d'instant sans virement en vol. Chaque compte est atomique, le virement ne l'est pas : un plantage entre le débit et le crédit perdrait l'argent, et un lecteur le voit manquer.

**`orderedLock` : la somme peut dépasser 5 000 000.** `total()` lit les comptes un par un, sans verrou. Exemple : le compte 5 a 200 et vire 150 au compte 500. `total()` lit le compte 5 (200 : les 150 y sont encore). Puis le virement s'exécute entièrement. Quand `total()` arrive au compte 500, les 150 y sont déjà : ils sont comptés deux fois. Dans l'autre sens (500 → 5), les 150 ne sont comptés nulle part et la somme est trop basse. Chaque virement est atomique, la lecture ne l'est pas (atomicité sur plusieurs comptes, pas des valeurs périmées). Correction : verrouiller tous les comptes dans l'ordre dans `total()`, ou prendre le verrou global.

**`globalLock` : exact,** parce que `total()` prend le même verrou que `transfer()`.

**S1 : un `SELECT sum(balance_minor)` pendant les virements aurait toujours été exact.** Il lit une photo (*snapshot*) prise au début de la requête. Pour chaque ligne, PostgreSQL renvoie la version visible dans cette photo (selon `xmin` et `xmax`). Un virement est une seule transaction qui valide ses deux lignes en même temps : la photo voit les deux nouvelles versions, ou aucune. Et les lecteurs ne bloquent pas les écrivains : la cohérence est gratuite. En mémoire, je ne l'obtiens qu'avec un verrou qui couvre les deux comptes (ou tous, pour une lecture), ou avec un grand livre.

### Constats

- **Les échecs de CAS suivent la contention, comme l'optimiste de S1, mais à un niveau bien plus bas :** 8,7 % de rejeux par virement accepté à 10 comptes, 0,27 % à 1 000 (≈ 30 fois moins). La fenêtre de course va de `balances.get(from)` au CAS : quelques nanosecondes, et `Work.simulate()` est **en dehors**. En S1, la fenêtre durait toute une transaction (des millisecondes).
- **Pas la même métrique qu'en S1 :** les 44 % de S1 (10 comptes) étaient des **échecs définitifs après 3 rejeux** ; ici, ce sont des **rejeux**, et une boucle CAS n'abandonne jamais (0 échec définitif).
- **Avec `--work=20`, encore moins d'échecs de CAS** (491 au lieu de 71 472 à 10 comptes) : les threads passent presque tout leur temps endormis dans `parkNanos`, donc peu d'entre eux sont dans la fenêtre en même temps.
- **« Un CAS coûte 0 » tient à cette échelle :** sans travail, 10 et 1 000 comptes donnent le même débit (≈ 4,7 M et 5,3 M/s) malgré 30 fois plus d'échecs. Même à 100 ns par échec, 71 000 échecs coûtent ≈ 7 ms sur 175 ms.
- **L'argent en vol fait refuser des virements légitimes :** à 10 comptes avec `--work=20`, **87 % des virements sont refusés** (contre 8,6 % sans travail). La banque détient 50 000, mais 1 000 threads × 500 en moyenne peuvent garder jusqu'à ≈ 500 000 en vol : les comptes sont presque toujours vides. L'invariant tient, et pourtant un client qui a assez d'argent recevrait « fonds insuffisants ».
- **À 1 000 comptes, le moteur atomique atteint le plafond du banc** (78 915/s ≈ 80 000/s) : 2,3 fois le verrou global, 1,8 fois le moteur ordonné. La limite n'est plus le moteur.
- **L'audit mesure directement les virements en vol :** ≈ 504 000 / 500 ≈ **1 000 virements en vol**, c'est-à-dire presque tous les threads entre leur débit et leur crédit. Avec la loi de Little, 1 000 en vol / 79 000 par seconde ≈ **12,7 ms par virement**, et non 28 µs. Le « ≈ 2,2 en vol » de mercredi (calculé avec 28 µs) comptait les virements *en train de travailler*, pas les virements *commencés et pas finis*. Interprétation (à vérifier) : après ses 20 µs de `parkNanos`, un thread classique attend ≈ 12 ms pour retrouver un cœur ; c'est le coût de réveil par l'OS derrière le plafond de ≈ 80 000/s.

### Bilan de la prédiction

| Prédiction | Verdict |
| --- | --- |
| 10 comptes, `--work=0` : 500 M/s | ❌ ≈ 4,7 M/s. Erreur d'unité : 1 / 0,2 µs = 5 M/s, donc le raisonnement donnait 50 M/s, pas 500 M/s. Sans travail, la limite, ce sont les 10 cœurs et le banc lui-même, pas les comptes |
| 10 comptes, `--work=20` : 357 000/s | ❌ 91 889/s, avec 87 % de refus. Le moteur ne tient rien pendant le travail : le nombre de comptes ne limite pas le parallélisme ; l'argent en vol, si |
| 1 000 comptes, `--work=0` : 50 G/s | ❌ ≈ 5,3 M/s. Pas de contrôle de vraisemblance : 10 cœurs ne font pas 50 milliards de virements par seconde, et 900 000 virements dureraient 18 µs |
| 1 000 comptes, `--work=20` : 35,7 M/s | ❌ 78 915/s : le plafond du banc (réveil des threads par l'OS) |
| « Un CAS coûte 0 » | ✅ cohérent avec les résultats à cette échelle |
| Échecs de CAS, audit | non écrits |

La méthode de prédiction (quelle ressource limite, formule du plafond, unités, contrôle de vraisemblance) est un point de travail pour les prochaines séances.

### Questions ouvertes

1. Où un thread classique passe-t-il ≈ 12,7 ms par virement, quand le travail dure 28 µs ? À vérifier avec `--virtual` (vendredi) : si le coût de réveil est la limite, le débit et l'argent en vol devraient beaucoup changer.
2. Le taux de refus de 87 % à 10 comptes baisse-t-il avec `--virtual` ou avec moins de threads (moins d'argent en vol) ?

---

## Vendredi 16 — `ShardedQueueEngine` (un seul écrivain par compte)

Mesuré le dimanche 18. `ShardedQueueEngine` : ni verrou, ni atomique. `long[] balances` et N exécuteurs à un seul thread (`Executors.newSingleThreadExecutor()`), avec N = `Runtime.getRuntime().availableProcessors()` = **10** sur ce Mac. Le compte `id` appartient toujours au shard `id % N` : **un seul thread écrit dans un compte donné**. Un virement, ce sont deux messages : le débit est envoyé au shard de l'émetteur (test du solde, débit, `Work.simulate()`), et l'appelant l'attend avec `Future.get()` ; la tâche de débit envoie ensuite le crédit au shard du destinataire avec `execute()`, sans l'attendre. `drain()` envoie une tâche vide à chaque shard et l'attend (les files sont FIFO, donc tous les crédits déjà en file sont appliqués), puis arrête le shard. `total()` lit les comptes un par un, sans verrou.

JUnit : `shardedEngine` ajouté au `@ValueSource`, **vert**. Code relu avant de mesurer : aucun bug trouvé. `drain()` déclare maintenant `throws InterruptedException` au lieu de l'envelopper.

### Prédiction (avant mesure)

| Lancement | Prédiction | Raisonnement |
| --- | --- | --- |
| 10 comptes, `--work=0` : refus, `queue` contre `orderedLock` | `queue` refuse **moins** | non écrit |

### Méthode

`EngineLab --threads=1000 --transfers=1000 --accounts=10 --work=0 --audit`, threads classiques, 3 lancements par moteur, médiane gardée. Les deux moteurs ont été lancés avec `--audit` (pas `orderedLock` mercredi) : ils se comparent seulement entre eux.

### Résultats (médianes de 3 lancements)

| Moteur | Durée | Acceptés | Refusés | Acceptés/s | Audit min / max | Dispersion (min–max) |
| --- | ---: | ---: | ---: | ---: | --- | --- |
| `orderedLock` | 365 ms | 823 255 | 76 625 (**8,5 %**) | ≈ 2,25 M | 48 076 / 51 200 (pire des 3 lancements) | 345–380 ms |
| `queue` | **16 393 ms** | 207 389 | 692 596 (**77 %**) | 12 591 | **327** / 50 000 (pire des 3 lancements) | 16 320–16 640 ms |

Invariant `OK` dans les 6 lancements. Minimum de l'audit de `queue` par lancement : 420, 345, 327.

### Pourquoi `queue` refuse autant de virements

**L'argent est dans les files de crédits, pas dans les soldes.** L'audit a vu **327 sur 50 000** à un moment : **99,3 % de l'argent était en vol** (débité, crédit pas encore appliqué).

1. **Un crédit attend derrière environ 100 débits.** Les 1 000 appelants ont chacun un débit en attente (ils attendent sur `get()`), répartis sur 10 shards : environ 100 débits par shard. Avec 10 comptes et 10 shards, c'est à peu près un compte par shard. Un crédit envoyé à un shard se place au bout de cette file.
2. **Ces débits voient un solde vide.** Un solde démarre à 5 000 et un virement fait environ 500 en moyenne : environ 10 débits vident un compte. Dès qu'un crédit est appliqué, les débits suivants de la file le reprennent. L'argent passe très peu de temps dans un solde, et longtemps dans une file.
3. **Le phénomène s'auto-entretient.** Un refus est très rapide, donc l'appelant renvoie aussitôt un nouveau débit, et la file devant chaque crédit reste pleine.

**Contrôle de vraisemblance :** le modèle dit que seule une minorité de débits peut passer (environ 10 à chaque réapprovisionnement, sur environ 100 débits en file) : quelques dizaines de pour cent au plus. Mesuré : 207 389 / 899 985 ≈ **23 % acceptés**. Même ordre de grandeur : le modèle tient.

**`orderedLock` n'a pas ce problème :** le débit et le crédit se font dans la même section critique, sous les deux verrous, donc l'argent n'est en vol que pendant la durée d'un virement. Les écarts de son audit (48 076 à 51 200) viennent seulement de la lecture compte par compte, sans instantané (voir jeudi). Le maximum de `queue` vaut exactement 50 000 : c'est la première lecture, avant que l'argent ne commence à circuler.

### Constats

- **Pas de verrou, pas de deadlock, pas de rejeu de CAS, et pourtant le pire moteur à 10 comptes :** 77 % des virements refusés, contre 8,5 % pour `orderedLock`. L'invariant tient, et pourtant des clients qui ont assez d'argent reçoivent « fonds insuffisants ».
- **Même mécanisme que le moteur atomique, poussé à l'extrême :** jeudi, `atomicEngine` à 10 comptes avec `--work=20` refusait 87 %, parce que l'argent était en vol pendant `Work.simulate()`. Ici, il n'y a aucun travail, et l'argent est en vol parce que le crédit attend dans une file.
- **Un seul écrivain par compte supprime les courses, pas l'écart entre débit et crédit.** Chaque compte est cohérent ; le virement n'est pas atomique. C'est exactement ce que donne un topic Kafka partitionné par `account_id` : l'ordre par compte, aucun verrou, et de l'argent en vol entre deux partitions.
- **Beaucoup plus lent :** 12 591 acceptés/s contre ≈ 2,25 M/s. Comparer les acceptés/s est biaisé (77 % de refus), donc on compte les appels traités : (acceptés + refusés) / durée ≈ 899 985 / 16,4 s ≈ **55 000/s**, toujours environ 45 fois plus lent que `orderedLock` (≈ 2,5 M appels/s). Avec la loi de Little : 1 000 appelants en attente / 55 000 par seconde ≈ **18 ms par appel**, pour quelques nanosecondes de vrai travail. Hypothèse (à vérifier) : chaque virement est un aller-retour entre threads (file, `get()`, réveil de l'appelant), et 1 000 threads classiques se disputent 10 cœurs ; même ordre de grandeur que les ≈ 12,7 ms par virement de jeudi.

### Bilan de la prédiction

| Prédiction | Verdict |
| --- | --- |
| `queue` refuse moins de virements que `orderedLock` à 10 comptes | ❌ **9 fois plus** (77 % contre 8,5 %). Les refus ne viennent pas des conflits, mais de ce que voit le débit : l'argent en vol fait paraître les soldes plus bas qu'ils ne sont |

### Questions ouvertes

1. Pourquoi `balances` n'a besoin ni de `volatile` ni d'un atomique, alors que 10 threads de shard utilisent le même tableau ? (Quel thread écrit dans quelle case, et quelle relation *happens-before* rend le `total()` final correct ?)
2. Pendant `--audit`, `total()` lit `balances` depuis le thread principal, sans *happens-before* avec les shards : peut-il voir des valeurs périmées ? Qu'est-ce que ça change au min et au max ?
3. Où passent les ≈ 18 ms par appel ? Même cause que les ≈ 12,7 ms de jeudi ? À vérifier avec `--virtual`.
4. Non lancés (sautés à cause du retard de la semaine) : `--virtual` et `--hot=50` pour chaque moteur correct.

---

## Samedi 17 — Comparaison des moteurs

Mesuré le dimanche 18. Cases manquantes remplies pour `queue` (1 000 comptes), lenteur de `queue` étudiée, et `orderedLock` relancé avec un `total()` verrouillé. Colonnes « compte chaud » et « threads virtuels » du tableau prévu sautées (retard de la semaine), sauf pour l'étude de `queue`.

### Prédiction (avant mesure)

| Lancement | Prédiction | Raisonnement |
| --- | --- | --- |
| `queue`, 1 000 comptes, `--work=0` | Pas de différence avec 10 comptes | Un crédit attend derrière 1 000 threads / N débits ; le nombre de comptes n'y change rien (métrique non nommée) |
| `queue`, 1 000 comptes, `--work=20` | Plus de 30 s, `STUCK` | 45 fois plus lent que `orderedLock` à `--work=0` |
| `queue`, `--work=20`, `--transfers=100` (100 000 virements) | ≈ 0,29 s | Plafond : 10 shards / 28 µs ≈ 357 000/s |
| Idem, `--virtual` et `--threads=100` | non écrite | Coût du réveil d'un thread inconnu |
| `orderedLock` avec un `total()` verrouillé | Débit plus bas (sans chiffre) ; audit min = max = 5 000 000 | Prendre et relâcher tous les verrous bloque les virements |

### Méthode

`EngineLab --threads=1000 --transfers=1000 --audit`, threads classiques sauf mention contraire, 3 lancements et médiane quand c'est indiqué, sinon un seul lancement. Pour le `total()` verrouillé, `OrderedLockEngine.total()` prend les 1 000 verrous par id croissant (le même ordre que `transfer()`, donc pas de deadlock), additionne, puis les relâche.

### Résultats : `queue`, 1 000 comptes, `--work=0` (médianes de 3 lancements)

| Durée | Acceptés | Refusés | Acceptés/s | Audit min / max (pire des 3 lancements) | Dispersion (min–max) |
| ---: | ---: | ---: | ---: | --- | --- |
| 18 280 ms | 901 942 | 97 048 (**9,7 %**) | 49 340 | 4 508 253 / 5 000 000 | 18 203–18 557 ms |

Invariant `OK` dans les 3 lancements. À comparer avec 10 comptes (vendredi) : 16 393 ms, 77 % de refus, audit min 327 / 50 000.

### Résultats : `queue`, 1 000 comptes, `--work=20`

Avec `--transfers=1000` et des threads classiques, 3 lancements sur 3 se sont terminés par `STUCK after 30 s` (acceptés = 840 230 ; 854 967 ; 842 957, soit ≈ 28 000/s) et `deadlocked threads=0` : un **dépassement du délai, pas un deadlock**. Le moteur avançait toujours.

Avec `--transfers=100` (100 000 virements), un lancement chacun :

| Appelants | Durée | Acceptés | Refusés | Acceptés/s | Part du plafond de 357 000/s | Audit min | Argent en vol (max) |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 000 threads classiques | 3 199 ms | 92 606 | 7 297 (7,3 %) | 28 948 | 8 % | 4 471 838 | 10,6 % |
| 1 000 threads **virtuels** | 673 ms | 92 447 | 7 454 (7,5 %) | **137 365** | 38 % | 4 476 012 | 10,5 % |
| **100** threads classiques (`--transfers=1000`) | 869 ms | 93 675 | 6 220 (6,2 %) | 107 796 | 30 % | 4 934 842 | **1,3 %** |

Avec `--virtual` et `--transfers=1000` (un lancement) : 5 601 ms, 900 863 acceptés, **160 839/s**.

Invariant `OK` dans tous les lancements terminés.

### Résultats : `orderedLock` avec un `total()` verrouillé (1 000 comptes, `--work=20`, un lancement)

| `total()` | Durée | Acceptés | Refusés | Acceptés/s | Audit min / max |
| --- | ---: | ---: | ---: | ---: | --- |
| Sans verrou (médiane de mercredi ; audit d'un lancement du dimanche 18) | 20 688 ms | 906 830 | 92 205 | 43 833 | 4 997 463 / 5 002 828 |
| **Tous les verrous, ordre croissant** | 19 443 ms | 908 554 | 90 441 | 46 729 | **5 000 000 / 5 000 000** |

### Tableau comparatif

Acceptés/s, médianes de 3 lancements, `--threads=1000 --transfers=1000`, threads classiques sauf mention contraire.

| Moteur | 10 comptes, work 0 | 1 000 comptes, work 0 | 1 000 comptes, work 20 | Audit cohérent ? (1 000 comptes) | Deadlock possible ? |
| --- | ---: | ---: | ---: | --- | --- |
| `naive` | `BROKEN` (+621 542) | `BROKEN` (+34 033) | non lancé | — (invariant cassé) | Non |
| `globalLock` | ≈ 3,7 M | ≈ 5,3 M | 34 489 | ✅ 5 000 000 / 5 000 000 | Non |
| `unorderedLock` | `STUCK` (`--work=20`, 13 virements) | non lancé | non lancé | — (`STUCK`) | **Oui** |
| `orderedLock` | ≈ 2,4 M | ≈ 4,6 M | 43 833 (46 729 avec un `total()` verrouillé, un lancement) | ❌ lecture sans verrou : 4 997 463 / 5 002 828 ; ✅ lecture verrouillée : 5 000 000 / 5 000 000 | Non (ordre fixe) |
| `atomicEngine` | ≈ 4,7 M | ≈ 5,3 M | **78 915** | ❌ 4 496 159 / 4 997 389 (argent en vol) | Non |
| `queue` | 12 591 (77 % refusés) | 49 340 | > 30 s (délai dépassé) ; 160 839 avec `--virtual` (un lancement) | ❌ 4 508 253 / 5 000 000 à `--work=0` (argent en vol) | Non |

Sources : lundi (`naive`, écart final), mercredi (`globalLock` sans `--audit`, `orderedLock`, `unorderedLock`), jeudi (`atomicEngine`, audit de `globalLock`), vendredi et dimanche 18 (`queue`, audits d'`orderedLock`).

### Constats

- **L'argent en vol de `queue` dépend du nombre d'appelants, pas du type de thread.** Chaque appelant a au plus un virement en vol, d'environ 500 en moyenne : 1 000 appelants × 500 ≈ 500 000, soit **10 % de 5 000 000** (mesuré 10,5–10,6 %) ; 100 appelants × 500 ≈ 50 000, soit **1 %** (mesuré 1,3 %). À 10 comptes, 500 000 dépasse les 50 000 de la banque, d'où 99 % en vol vendredi. C'est la loi de Little : en vol = débit × temps passé dans le système.
- **Le prix de l'argent en vol dépend de la contention par compte, pas du débit.** `queue` met à peu près le même temps avec 10 ou 1 000 comptes (16,4 s contre 18,3 s), mais refuse 77 % contre 9,7 % : environ 100 débits attendent par compte à 10 comptes, environ 1 à 1 000.
- **La lenteur de `queue` venait des appelants en threads classiques, pas des files.** Même moteur, mêmes 1 000 appelants : 4,7 fois plus rapide en threads virtuels (28 948 → 137 365/s), 3,7 fois plus rapide avec 10 fois moins de threads classiques (107 796/s). Réveiller un thread virtuel ne passe pas par le système. Cela répond aussi à la question 1 de jeudi : en threads virtuels, `queue` dépasse le plafond du banc d'essai (≈ 80 000/s en threads classiques).
- **`STUCK` n'est pas toujours un deadlock.** Avec `queue` à `--work=20`, le banc d'essai a dépassé son délai avec `deadlocked threads=0` et ≈ 840 000 virements faits : le moteur était lent, pas bloqué.
- **Les virements d'`orderedLock` sont atomiques ; seule sa lecture ne l'était pas.** Avec un `total()` qui prend tous les verrous dans le même ordre, l'audit lit exactement 5 000 000 à chaque fois. `atomicEngine` ne se corrige pas de cette façon : il n'y a aucun verrou à prendre, l'argent est vraiment dans un thread entre le CAS et le crédit.
- **La lecture cohérente sous tous les verrous n'a rien coûté de mesurable ici** (46 729/s sur un lancement, contre 43 833/s en médiane) : un audit par milliseconde, un seul thread. Son coût grandit avec la fréquence des lectures et avec le nombre de comptes.
- **À `--work=20`, `atomicEngine` est limité par le plafond du banc d'essai** (78 915/s ≈ 80 000/s en threads classiques) : l'écart de 1,8 fois avec `orderedLock` est une borne basse.

### Bilan de la prédiction

| Prédiction | Verdict |
| --- | --- |
| `queue` : pas de différence entre 10 et 1 000 comptes | ✅ pour la durée (16,4 contre 18,3 s), ❌ pour les refus (77 % contre 9,7 %) et l'argent en vol (99 % contre 10 %). La métrique n'était pas nommée |
| `queue`, `--work=20` : plus de 30 s, `STUCK` | ✅ mais pour une mauvaise raison : un rapport mesuré à `--work=0` ne se transpose pas à une autre configuration. La formule du plafond (357 000/s) disait que le délai ne serait **pas** dépassé |
| `queue`, 100 000 virements : ≈ 0,29 s | ❌ 3 199 ms (11 fois plus lent) : la limite venait des appelants, pas des shards |
| `orderedLock`, `total()` verrouillé : débit plus bas | Non visible (un lancement, +7 %, dans le bruit). Sans ordre de grandeur, la prédiction ne pouvait pas être vérifiée ; une borne l'aurait permis : durée d'une lecture (quelques dizaines de µs) / 1 ms entre deux lectures → quelques % au plus |
| `orderedLock`, `total()` verrouillé : audit min = max = 5 000 000 | ✅ |

### Questions ouvertes

1. En threads virtuels, `queue` n'atteint que 38 % du plafond de 357 000/s : les crédits passent aussi par les files, et `parkNanos` est moins précis sous charge. Non étudié.
2. `atomicEngine` et `orderedLock` avec `--virtual` : l'écart entre les deux grandit-il une fois le plafond du banc d'essai levé ?
3. Combien de temps dure un `total()` verrouillé (`System.nanoTime()` autour, 3 lancements) ?

---

## Annexe — `jstack`, `unorderedLock`, 10 comptes

```
Found one Java-level deadlock:
=============================
"pool-1-thread-1":
  waiting for ownable synchronizer 0x000000070e85f650, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
  which is held by "pool-1-thread-8"

"pool-1-thread-8":
  waiting for ownable synchronizer 0x000000070e85f740, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
  which is held by "pool-1-thread-9"

"pool-1-thread-9":
  waiting for ownable synchronizer 0x000000070e85f7a0, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
  which is held by "pool-1-thread-15"

"pool-1-thread-15":
  waiting for ownable synchronizer 0x000000070e85f740, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
  which is held by "pool-1-thread-9"
```

Chaque pile du cycle se termine par `UnorderedLockEngine.transfer` → `UnorderedLockEngine.lock` → `ReentrantLock.lock` → `LockSupport.park`.
