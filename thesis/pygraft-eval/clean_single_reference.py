import json
import sys
import time
import urllib.request

BASE = "http://localhost:8080"
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]

def build(cfg, variant, seed):
    return {
        "entityCounts": cfg["entityCounts"], "constructs": cfg["constructs"], "seed": seed,
        "structure": {"hierarchyTargetDepth": cfg["structure"]["hierarchyTargetDepth"],
                      "hierarchyBranchingFactor": cfg["structure"]["hierarchyBranchingFactor"]},
        "variants": [variant], "targetProfile": "DL", "strictConsistency": True,
    }

def main():
    ref = sys.argv[1]
    configs = json.load(open("reference_configs.json"))
    cfg = configs[ref]

    try:
        results = json.load(open("clean_reference_results.json"))
    except FileNotFoundError:
        results = {}

    for variant in VARIANTS:
        for seed in range(1, 11):
            key = f"{ref}_{variant}_{seed}"
            if key in results:
                continue
            req = build(cfg, variant, seed)
            t0 = time.time()
            r = urllib.request.Request(BASE + "/api/generations", data=json.dumps(req).encode(),
                                        headers={"Content-Type": "application/json"}, method="POST")
            with urllib.request.urlopen(r) as resp:
                job = json.loads(resp.read())
            job_id = job["jobId"]
            while True:
                with urllib.request.urlopen(BASE + f"/api/generations/{job_id}") as resp:
                    job = json.loads(resp.read())
                if job["status"] in ("COMPLETED", "FAILED"):
                    break
                time.sleep(0.05)
            elapsed = time.time() - t0
            v = job.get("variants", [{}])[0]
            entry = {"status": job["status"], "elapsed_s": elapsed}
            entry["verif_status"] = (v.get("verification") or {}).get("status")
            m = v.get("metrics") or {}
            if m:
                h = m.get("hierarchy", {})
                entry["hierarchy_depth"] = h.get("maxDepth")
                entry["branching"] = h.get("avgBranchingFactor")
                entry["tangledness"] = h.get("tangledness")
                g = m.get("graph", {})
                entry["avg_degree"] = g.get("avgDegree")
                entry["clustering"] = g.get("clusteringCoefficient")
                entry["inferred_ratio"] = m.get("reasoning", {}).get("inferredToAssertedRatio")
            results[key] = entry
            print(f"{key}: {entry['verif_status']} in {elapsed:.2f}s", flush=True)
            json.dump(results, open("clean_reference_results.json", "w"), indent=2)

if __name__ == "__main__":
    main()
