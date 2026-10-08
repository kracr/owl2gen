import json
import time
import sys
import urllib.request

BASE = "http://localhost:8080"
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]
ALL_CONSTRUCTS = json.load(open("all_constructs.json"))

def post(path, payload):
    data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def get(path):
    with urllib.request.urlopen(BASE + path) as resp:
        return json.loads(resp.read())

def build_request(per_construct, variant, seed, strict=True):
    return {
        "entityCounts": {"classes": 20, "objectProperties": 6, "dataProperties": 3, "individuals": 10},
        "constructs": {c: per_construct for c in ALL_CONSTRUCTS},
        "seed": seed,
        "structure": {"hierarchyTargetDepth": 5, "hierarchyBranchingFactor": 3},
        "variants": [variant],
        "targetProfile": "DL",
        "strictConsistency": strict,
    }

def run_one(name, req, timeout_s=400):
    t0 = time.time()
    job = post("/api/generations", req)
    job_id = job["jobId"]
    while True:
        if time.time() - t0 > timeout_s:
            return {"name": name, "status": "TIMEOUT", "elapsed_s": time.time() - t0}
        job = get(f"/api/generations/{job_id}")
        if job["status"] in ("COMPLETED", "FAILED"):
            break
        time.sleep(0.05)
    elapsed = time.time() - t0
    result = {"name": name, "status": job["status"], "elapsed_s": elapsed}
    if job.get("variants"):
        v = job["variants"][0]
        result["axiomCount"] = v.get("axiomCount")
        result["variant_status"] = v.get("status")
        result["errorMessage"] = v.get("errorMessage")
        result["verif_status"] = (v.get("verification") or {}).get("status")
        m = v.get("metrics") or {}
        if m:
            h = m.get("hierarchy", {})
            result["hierarchy_depth"] = h.get("maxDepth")
            result["branching"] = h.get("avgBranchingFactor")
            result["tangledness"] = h.get("tangledness")
            g = m.get("graph", {})
            result["avg_degree"] = g.get("avgDegree")
            result["clustering"] = g.get("clusteringCoefficient")
            result["inferred_ratio"] = m.get("reasoning", {}).get("inferredToAssertedRatio")
            result["nesting_max_depth"] = m.get("nesting", {}).get("maxDepth")
    return result

def main():
    stage = sys.argv[1] if len(sys.argv) > 1 else "all"
    try:
        results = json.load(open("basic_experiments_results.json"))
    except FileNotFoundError:
        results = {}

    def save():
        json.dump(results, open("basic_experiments_results.json", "w"), indent=2)

    # Experiment 1: Structural Variation across Repeated Runs -- 320 requested (5/construct), Uniform Random, 10 seeds
    if stage in ("all", "variation"):
        for seed in range(1, 11):
            key = f"variation_UNIFORM_RANDOM_{seed}"
            if key in results:
                continue
            req = build_request(5, "UNIFORM_RANDOM", seed, strict=True)
            r = run_one(key, req)
            results[key] = r
            print(key, r.get("verif_status", r["status"]), f"{r['elapsed_s']:.2f}s", flush=True)
            save()

    # Experiment 2 + 3 combined: scaling table -- 128/320/512 requested (2/5/8 per construct), 4 variants, 10 seeds
    if stage in ("all", "scaling"):
        for per_construct in (2, 5, 8):
            for variant in VARIANTS:
                for seed in range(1, 11):
                    key = f"scaling_{per_construct}_{variant}_{seed}"
                    if key in results:
                        continue
                    req = build_request(per_construct, variant, seed, strict=True)
                    r = run_one(key, req)
                    results[key] = r
                    print(key, r.get("verif_status", r["status"]), f"{r['elapsed_s']:.2f}s", flush=True)
                    save()

    # Off-mode control: 1600 requested (25/construct), strictConsistency=False, 4 variants, 3 runs each
    if stage in ("all", "offmode"):
        for variant in VARIANTS:
            for seed in range(1, 4):
                key = f"offmode_{variant}_{seed}"
                if key in results:
                    continue
                req = build_request(25, variant, seed, strict=False)
                r = run_one(key, req, timeout_s=60)
                results[key] = r
                print(key, r.get("verif_status", r["status"]), f"{r['elapsed_s']:.2f}s", flush=True)
                save()

if __name__ == "__main__":
    main()
