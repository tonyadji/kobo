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
