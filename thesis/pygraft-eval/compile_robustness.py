import json
import statistics as stats

data = json.load(open("robustness_results.json"))

def median_iqr(values):
    if not values:
        return None, None, None
    sorted_v = sorted(values)
    n = len(sorted_v)
    med = stats.median(sorted_v)
    def pct(p):
        idx = p * (n - 1)
        lo = int(idx)
        hi = min(lo + 1, n - 1)
        frac = idx - lo
        return sorted_v[lo] * (1 - frac) + sorted_v[hi] * frac
    q1, q3 = pct(0.25), pct(0.75)
    return med, q1, q3

SIZES = ["128", "320", "512"]
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]

print(f"{'Key':<22} {'N':>3} {'Consist':>8} {'TimeMed':>9} {'TimeIQR':>18} {'DepthMed':>9} {'BranchMed':>10} {'TangledMed':>11} {'AvgDegMed':>10} {'ClustMed':>9} {'RatioMed':>9}")
summary = {}
for size in SIZES:
    for variant in VARIANTS:
        key = f"{size}_{variant}"
        runs = data.get(key, [])
        n = len(runs)
        consistent = [r for r in runs if r.get("verif_status") == "CONSISTENT"]
        n_consistent = len(consistent)

        times = [r["elapsed_s"] for r in runs if r.get("elapsed_s") is not None]
        t_med, t_q1, t_q3 = median_iqr(times)

        depths = [r["hierarchy_depth"] for r in consistent if r.get("hierarchy_depth") is not None]
        branch = [r["branching"] for r in consistent if r.get("branching") is not None]
        tangled = [r["tangledness"] for r in consistent if r.get("tangledness") is not None]
        avgdeg = [r["avg_degree"] for r in consistent if r.get("avg_degree") is not None]
        clust = [r["clustering"] for r in consistent if r.get("clustering") is not None]
        ratio = [r["inferred_ratio"] for r in consistent if r.get("inferred_ratio") is not None]

        d_med, _, _ = median_iqr(depths)
        b_med, _, _ = median_iqr(branch)
        tg_med, _, _ = median_iqr(tangled)
        ad_med, _, _ = median_iqr(avgdeg)
        cl_med, _, _ = median_iqr(clust)
        r_med, _, _ = median_iqr(ratio)

        summary[key] = {
            "n": n, "n_consistent": n_consistent,
            "time_median": t_med, "time_q1": t_q1, "time_q3": t_q3,
            "depth_median": d_med, "branch_median": b_med, "tangled_median": tg_med,
            "avgdeg_median": ad_med, "clust_median": cl_med, "ratio_median": r_med,
            "failures": [{"seed": r["seed"], "status": r.get("verif_status"), "time": r["elapsed_s"]} for r in runs if r.get("verif_status") != "CONSISTENT"],
        }

        print(f"{key:<22} {n:>3} {n_consistent:>3}/10   {t_med:>7.3f}s  [{t_q1:>6.3f},{t_q3:>6.3f}]s  "
              f"{d_med:>9.1f} {b_med:>10.2f} {tg_med:>11.2f} {ad_med:>10.2f} {cl_med:>9.3f} {r_med:>9.2f}")

print()
print("=== Failures (non-CONSISTENT outcomes) ===")
for key, s in summary.items():
    if s["failures"]:
        print(f"{key}: {s['failures']}")

with open("robustness_summary.json", "w") as f:
    json.dump(summary, f, indent=2, default=str)
