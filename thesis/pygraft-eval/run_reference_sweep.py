import json
import time
import sys
import urllib.request

BASE = "http://localhost:8080"
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]
SEEDS = list(range(1, 11))

def post(path, payload):
    data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def get(path):
    with urllib.request.urlopen(BASE + path) as resp:
        return json.loads(resp.read())

def build_request(cfg, seed, variant):
    return {
        "entityCounts": cfg["entityCounts"],
        "constructs": cfg["constructs"],
        "seed": seed,
        "structure": {
            "hierarchyTargetDepth": cfg["structure"]["hierarchyTargetDepth"],
            "hierarchyBranchingFactor": cfg["structure"]["hierarchyBranchingFactor"],
        },
        "variants": [variant],
        "targetProfile": "DL",
        "strictConsistency": True,
    }

def run_one(name, req, timeout_s=1800):
    t0 = time.time()
    job = post("/api/generations", req)
    job_id = job["jobId"]
    while True:
        elapsed = time.time() - t0
        if elapsed > timeout_s:
            return {"name": name, "status": "TIMEOUT", "elapsed_s": elapsed}
        job = get(f"/api/generations/{job_id}")
        if job["status"] in ("COMPLETED", "FAILED"):
            break
        time.sleep(0.05)
    elapsed = time.time() - t0
    result = {"name": name, "status": job["status"], "elapsed_s": elapsed}
    if job.get("error"):
        result["error"] = job["error"]
    if job.get("variants"):
        v = job["variants"][0]
        result["verif_status"] = v.get("verification", {}).get("status")
        m = v.get("metrics", {})
        if m:
            h = m.get("hierarchy", {})
            g = m.get("graph", {})
            r = m.get("reasoning", {})
            n = m.get("nesting", {})
            result["hierarchy_depth"] = h.get("maxDepth")
            result["branching"] = h.get("avgBranchingFactor")
            result["tangledness"] = h.get("tangledness")
            result["avg_degree"] = g.get("avgDegree")
            result["clustering"] = g.get("clusteringCoefficient")
            result["inferred_ratio"] = r.get("inferredToAssertedRatio")
            result["nesting_max_depth"] = n.get("maxDepth")
            result["nesting_avg_depth"] = n.get("avgDepth")
    return result

def main():
    configs = json.load(open("reference_configs.json"))
    refs = sys.argv[1:] if len(sys.argv) > 1 else list(configs.keys())

    try:
        results = json.load(open("reference_sweep_results.json"))
    except FileNotFoundError:
        results = {}

    for ref in refs:
        cfg = configs[ref]
        for variant in VARIANTS:
            for seed in SEEDS:
                key = f"{ref}_{variant}_{seed}"
                if key in results:
                    continue
                req = build_request(cfg, seed, variant)
                result = run_one(key, req)
                results[key] = result
                status = result.get("verif_status", result["status"])
                print(f"{key:<45} {status:<12} {result['elapsed_s']:.3f}s", flush=True)
                with open("reference_sweep_results.json", "w") as f:
                    json.dump(results, f, indent=2)

if __name__ == "__main__":
    main()
