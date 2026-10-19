# Kobo

A mini neobank to **measure, not guess**, how money moves safely under concurrency.

## What's inside

| Path | What |
| --- | --- |
| `db/` | PostgreSQL schema and seed: 1 M users, 1.2 M accounts, 5 M transactions |
| `lab/` | Five transfer strategies against PostgreSQL (atomic update, pessimistic, optimistic, serializable) |
| `engine/` | Six in-memory transfer engines in Java 21 (no lock, global lock, per-account locks, CAS, single writer per account) |
| `docs/adr/` | Decisions: [ADR-001](docs/adr/ADR-001.md) (database transfers), [ADR-003](docs/adr/ADR-003.md) (in-memory engine) |
| `docs/measurements/` | Every run, prediction and finding: [week 1](docs/measurements/s1.md), [week 2](docs/measurements/s2.md) |
| `katas/` | Timed live-coding exercises in TDD (load balancer, rate limiter, URL shortener, LRU cache) |
## Key findings

- **Lock ordering:** 150 s → 0.28 s for 800 transfers on 10 hot accounts, deadlocks gone.
- **No lock at all:** +1,606,478 of money created out of thin air in memory, 10 accounts.
- **One global lock** reaches 97% of its computed ceiling (1 / 28 µs ≈ 35,600 transfers/s).
- **CAS** is 1.8× faster than ordered locks with 20 µs of work, but no reader can see a consistent total.

## How to run

```bash
docker compose up -d          # PostgreSQL 17; schema and seed load on first start (~1 min)
```

In-memory engine (no database needed):

```bash
cd engine && mvn -q compile
java -cp target/classes com.lehnade.kobo.engine.EngineLab --engine=orderedLock --accounts=1000 --work=20 --audit
```

| Option | Default | Meaning |
| --- | --- | --- |
| `--engine` | `naive` | `naive`, `globalLock`, `unorderedLock`, `orderedLock`, `atomicEngine`, `queue` |
| `--accounts` | 10 | number of accounts, 5,000 each (fewer accounts = more contention) |
| `--threads` | 1000 | concurrent callers |
| `--transfers` | 1000 | transfers per caller |
| `--work` | 0 | simulated work inside a transfer, in µs |
| `--hot` | 0 | % of transfers that touch account 0 |
| `--virtual` | off | virtual threads instead of a fixed pool |
| `--audit` | off | reads the total every ms during the run (min / max) |

Database strategies (needs PostgreSQL running):

```bash
cd lab
mvn -q compile exec:java -Dexec.mainClass=com.lehnade.kobo.lab.TransferLab -Dexec.args="1000 3"
```

Arguments: number of accounts, number of runs. All five strategies run; results are appended to `lab/results.txt`.
With 10 accounts a run takes about 4 minutes: the unordered strategy deadlocks on purpose.
