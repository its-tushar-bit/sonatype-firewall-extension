# SBOM Monitoring Evaluation Performance Results

**Jira:** [CLM-38626](https://sonatype.atlassian.net/browse/CLM-38626)
**Parent:** [CLM-38616](https://sonatype.atlassian.net/browse/CLM-38616) - SBOM Manager: Continuous Monitoring for All Application Versions
**Date:** March–April 2026
**Author:** Richard Mealing

## Objective

Determine reasonable defaults for SBOM monitoring evaluation settings across different deployment sizes:

- How many policy evaluations per minute can the system handle?
- How many evaluation threads should be dedicated to SBOM monitoring?
- At what point does the system start to fail?

## Test Environments

### Local Environment

| Attribute | Value |
|-----------|-------|
| CPU | 32 cores (x86) |
| RAM | 24 GB |
| Database | PostgreSQL (local) |
| Storage | Local disk |
| Dataset | 1,000 apps, ~100k SBOMs (134 max versions/app) |

### GCP Production-Like Environment

| Attribute | Value |
|-----------|-------|
| Instance | GCE n4a-highmem-16 (16 ARM64 cores) |
| RAM | 125 GB (96 GB heap: `-Xms96g -Xmx96g`) |
| Database | Cloud SQL PostgreSQL 15 (8 vCPU, 30 GB) |
| Storage | NFS (Cloud Filestore) |
| Dataset | 33,615 apps, 107,757 active SBOMs (12 max versions/app) |

## Results

### Local Test

**Per-evaluation time:** ~11–12s

| `consumerThreadsPerTenant` | `consumerMaxQueuedRows` | Evals/hr | CPU % | Memory |
|:--------------------------:|:-----------------------:|:--------:|:-----:|:------:|
| 1 | 30 (new default) | ~192 | 0–2% | ~2.7 GB |
| 4 | 60 | ~768 | 0–7% | ~4.9 GB |
| 8 | 120 | ~1,536 | 0–12% | ~7.5 GB |
| 8 | 500 | ~2,484 | 2–8%* | ~8.8 GB |

\* Lower peak CPU in the 500-row case is due to work being spread more evenly across the poll interval vs concentrated bursts with smaller batches. Total CPU time is higher.

**Estimated cycle durations (1,000 apps):**

| Config | maxVersions=1 (1k evals) | All versions (~100k evals) |
|:------:|:------------------------:|:--------------------------:|
| 1 thread (default) | ~5 hours | ~520 hours |
| 4 threads | ~1.3 hours | ~130 hours |
| 8 threads | ~39 min | ~65 hours |

### GCP Test

**Per-evaluation time:** ~50s (~4x slower than local)

| `consumerThreadsPerTenant` | `consumerMaxQueuedRows` | Evals/hr | CPU (100% = 1 core) | Old Gen % |
|:--------------------------:|:-----------------------:|:--------:|:--------------------:|:---------:|
| 0 (baseline) | -- | -- | 6–14% | 0.00% |
| 1 | 30 | ~72 | 8–25% | 0.00% |
| 4 | 100 | ~300 | 21–48% | 0.04% |
| 8 | 200 | ~660 | 38–87% | 0.38% |

Baseline CPU of 6–14% is due to background Quartz jobs and search indexing across 33k apps.

**Estimated cycle durations (33,615 apps):**

| Config | maxVersions=1 (33k evals) | All versions (~108k evals) |
|:------:|:-------------------------:|:--------------------------:|
| 1 thread (default) | ~19.5 days | ~62 days |
| 4 threads | ~4.7 days | ~15 days |
| 8 threads | ~2.1 days | ~6.8 days |

### Heap Usage (GCP, sequential test, freshly restarted JVM)

| Phase | Old Gen % | Old Gen (est.) | YGC | Delta YGCT |
|:-----:|:---------:|:--------------:|:---:|:----------:|
| Baseline | 0.00% | 0 MB | 4 | -- |
| After 1 thread | 0.00% | ~0 MB | 12 | +0.215s |
| After 4 threads | 0.04% | ~38 MB | 17 | +0.640s |
| After 8 threads | 0.38% | ~365 MB | 22 | +0.575s |

Evaluation objects are short-lived — Old Gen barely moves. Each evaluation allocates ~1–1.2 GB total but objects are collected incrementally by Young GC. Concurrent live set per thread is ~0.5–1 GB.

### Local vs GCP Comparison

| Metric | Local (32-core x86, local storage) | GCP (16-core ARM, Cloud SQL + NFS) |
|--------|:----------------------------------:|:-----------------------------------:|
| Per-eval time | ~12s | ~50s |
| Scaling | Linear to 8 threads | Linear to 8 threads |
| CPU (8 threads) | 0–12% of 1 core | 38–87% of 1 core |
| Heap per thread | ~0.65 GB (Docker RSS) | Old Gen <1% (pre-allocated heap) |

## Key Findings

1. **Linear scaling confirmed.** Throughput scales linearly with `consumerThreadsPerTenant` — no bottleneck observed up to 8 threads in both environments. Version count has no effect on per-eval cost.

2. **I/O-bound workload.** 8 threads use less than 1 CPU core (87% peak where 100% = 1 core). Most time is spent waiting on database and storage I/O.

3. **Zero failures.** No failures or skips across all test phases.

4. **GCP is ~4x slower than local.** Per-eval time of ~50s vs ~12s, likely due to ARM architecture and remote storage latency (Cloud SQL + NFS vs local PostgreSQL + local disk).

5. **Memory is not a concern.** Evaluation objects are short-lived, Old Gen barely moves (<1% even at 8 threads). Resource impact per thread: <1% CPU per core, ~0.5–1 GB live heap.

6. **Scale challenge at default settings.** 1-thread default would take ~19.5 days per cycle for 33k apps (maxVersions=1). Customers at this scale need to tune threads up or use a version evaluation window.

7. **Background CPU is significant on large datasets** (~10%) — evaluation overhead should be measured relative to baseline, not zero.

## Recommended Defaults

### Configuration

| Setting | Default | Notes |
|---------|:-------:|-------|
| `consumerThreadsPerTenant` | **1** | Conservative, safe for on-prem and MTIQ |
| `consumerMaxQueuedRows` | **30** | Keeps threads busy between polls at 1 thread |

### Queue Sizing Formula

```
consumerMaxQueuedRows = consumerThreadsPerTenant × (consumerPeriodInMilliseconds / avgEvalTimeMs)
```

### Tuning Guidance by Deployment Size

| Deployment Size | Apps | Recommended Threads | Notes |
|:---------------:|:----:|:-------------------:|-------|
| Small | <1k | 1 (default) | Default is sufficient; completes a cycle in ~5 hours |
| Medium | 1k–10k | 2–4 | Reduces cycle time from days to hours |
| Large | 10k–30k | 4–8 | At 33k apps, 8 threads completes a cycle in ~2 days |
| Enterprise | 30k+ | 8+ | Consider version evaluation windows to limit scope |

### MTIQ Considerations

- Conservative 1-thread default allows 80 tenants to run simultaneously without overload
- Consumer poll jitter naturally spreads load across tenants
- Matches PolicyMonitor's single-thread throughput
- Note: enabling this by default monitors ALL SBOMs (not just latest) unless a version evaluation window is set

## HDS Impact

Additional load from SBOM monitoring evaluations was discussed with the HDS team. The recommendation is to monitor HDS response times if a large number of tenants enable monitoring simultaneously.