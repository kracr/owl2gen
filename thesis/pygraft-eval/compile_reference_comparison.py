import json
import statistics as stats

REFS = ["prov-o", "ssn", "sosa", "pizza", "goslim_generic", "wine"]
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]
METRIC_KEYS = ["hierarchy_depth", "branching", "tangledness", "avg_degree", "clustering", "inferred_ratio"]

REF_METRIC_MAP = {
    "hierarchy_depth": ("hierarchy", "maxDepth"),
    "branching": ("hierarchy", "avgBranchingFactor"),
    "tangledness": ("hierarchy", "tangledness"),
    "avg_degree": ("graph", "avgDegree"),
    "clustering": ("graph", "clusteringCoefficient"),
    "inferred_ratio": ("reasoning", "inferredToAssertedRatio"),
}

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
    return med, pct(0.25), pct(0.75)

def load_json_ignoring_log_lines(path):
    text = open(path).read()
    start = text.index("{")
    return json.loads(text[start:])

results = json.load(open("reference_sweep_results.json"))

summary = {}
for ref in REFS:
    ref_metrics = load_json_ignoring_log_lines(f"metrics-out/{ref}.json")
    print(f"\n{'='*100}")
    print(f"=== {ref} ===")
    ref_row = {}
    for mkey in METRIC_KEYS:
        section, field = REF_METRIC_MAP[mkey]
        ref_row[mkey] = ref_metrics[section][field]
    print(f"  reference values: {ref_row}")

    summary[ref] = {"reference": ref_row, "variants": {}}

    for var in VARIANTS:
        runs = [results[f"{ref}_{var}_{s}"] for s in range(1, 11)]
        statuses = [r.get("verif_status", r.get("status")) for r in runs]
        n_consistent = statuses.count("CONSISTENT")
        n_timeout = statuses.count("NOT_VERIFIED_TIMEOUT")
        consistent_runs = [r for r in runs if r.get("verif_status") == "CONSISTENT"]

        var_summary = {"n_consistent": n_consistent, "n_timeout": n_timeout, "metrics": {}}
        print(f"  --- {var}: {n_consistent}/10 consistent, {n_timeout}/10 timeout ---")
        for mkey in METRIC_KEYS:
            vals = [r[mkey] for r in consistent_runs if r.get(mkey) is not None]
            med, q1, q3 = median_iqr(vals)
            ref_val = ref_row[mkey]
            in_range = (q1 <= ref_val <= q3) if (med is not None and q1 is not None) else None
            var_summary["metrics"][mkey] = {"median": med, "q1": q1, "q3": q3, "ref_in_iqr": in_range}
            if med is not None:
                marker = "IN-IQR" if in_range else "OUT"
                print(f"    {mkey:<16} ref={ref_val:>8.3f}  gen_median={med:>8.3f}  IQR=[{q1:>7.3f},{q3:>7.3f}]  {marker}")
            else:
                print(f"    {mkey:<16} ref={ref_val:>8.3f}  gen_median=N/A (no consistent runs)")
        summary[ref]["variants"][var] = var_summary

with open("reference_comparison_summary.json", "w") as f:
    json.dump(summary, f, indent=2, default=str)
print("\nWrote reference_comparison_summary.json")
