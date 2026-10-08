"""
Addresses reviewer feedback on the "Structural Effects of Entity-Selection Strategies" and
"Scalability of Consistency-Aware Generation" subsubsections:
1. The existing attachment-strategy comparison used one seed per variant ("illustrative,
   does not estimate variation across seeds"). This reruns each (size, variant) combination
   across 10 seeds to get real median/IQR statistics and a real consistency rate.
2. The existing tables report only the *final* Openllet verification time (verification.elapsedMillis),
   not the cumulative cost of the per-construct-type reasoner calls Guarantee Consistency makes
   *during* generation. This measures true end-to-end wall-clock time via the async job API
   (submission to COMPLETED), which does include that cost.

Same three sizes as the existing tables: 2/5/8 axioms per construct across all 64 constructs
(128/320/512 requested), all four attachment strategies, seeds 1-10, Guarantee Consistency on,
DL profile -- otherwise identical to Tables tab:variant-comparison and tab:scaling-breakpoint.
"""
import json
import time
import sys
import urllib.request
import statistics

BASE = "http://localhost:8080"

with open("all_constructs.json") as f:
    ALL_CONSTRUCTS = json.load(f)

def post(path, payload):
    data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def get(path):
    with urllib.request.urlopen(BASE + path) as resp:
        return json.loads(resp.read())

def build_request(per_construct, variant, seed):
    constructs = {c: per_construct for c in ALL_CONSTRUCTS}
    return {
        "constructs": constructs,
        "seed": seed,
        "variants": [variant],
        "targetProfile": "DL",
        "strictConsistency": True,
    }

def run_one(req, timeout_s=400):
    t0 = time.time()
    job = post("/api/generations", req)
    job_id = job["jobId"]
    while True:
        elapsed = time.time() - t0
        if elapsed > timeout_s:
            return {"status": "TIMEOUT", "elapsed_s": elapsed}
        job = get(f"/api/generations/{job_id}")
        if job["status"] in ("COMPLETED", "FAILED"):
            break
        time.sleep(0.02)
    elapsed = time.time() - t0
    result = {"status": job["status"], "elapsed_s": elapsed}
    if job.get("variants"):
        v = job["variants"][0]
        result["actual_axioms"] = v.get("axiomCount")
        result["variant_status"] = v.get("status")
        verification = v.get("verification") or {}
        result["verif_status"] = verification.get("status")
        result["verif_elapsed_ms"] = verification.get("elapsedMillis")
        metrics = v.get("metrics") or {}
        if metrics:
            result["hierarchy_depth"] = metrics["hierarchy"]["maxDepth"]
            result["branching"] = metrics["hierarchy"]["avgBranchingFactor"]
            result["tangledness"] = metrics["hierarchy"]["tangledness"]
            result["avg_degree"] = metrics["graph"]["avgDegree"]
            result["clustering"] = metrics["graph"]["clusteringCoefficient"]
            reasoning = metrics.get("reasoning") or {}
            result["inferred_ratio"] = reasoning.get("inferredToAssertedRatio") if reasoning.get("computed") else None
    return result

SIZES = {"128": 2, "320": 5, "512": 8}
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]
SEEDS = list(range(1, 11))

def main():
    out_path = "robustness_results.json"
    try:
        with open(out_path) as f:
            all_results = json.load(f)
    except FileNotFoundError:
        all_results = {}

    size_filter = sys.argv[1] if len(sys.argv) > 1 else None
    variant_filter = sys.argv[2] if len(sys.argv) > 2 else None

    for size_label, per_construct in SIZES.items():
        if size_filter and size_label != size_filter:
            continue
        for variant in VARIANTS:
            if variant_filter and variant != variant_filter:
                continue
            key = f"{size_label}_{variant}"
            runs = []
            print(f"=== {key} (10 seeds) ===", flush=True)
            for seed in SEEDS:
                req = build_request(per_construct, variant, seed)
                r = run_one(req)
                r["seed"] = seed
                runs.append(r)
                print(f"  seed={seed}: {r.get('status')}/{r.get('verif_status')} "
                      f"time={r.get('elapsed_s', 0):.3f}s axioms={r.get('actual_axioms')}", flush=True)
            all_results[key] = runs
            with open(out_path, "w") as f:
                json.dump(all_results, f, indent=2, default=str)

if __name__ == "__main__":
    main()
