import json
import time
import urllib.request

BASE = "http://localhost:8080"
REFS = ["prov-o", "ssn", "sosa", "pizza", "goslim_generic", "wine"]
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]

def post(path, payload):
    data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def get(path):
    with urllib.request.urlopen(BASE + path) as resp:
        return json.loads(resp.read())

def run_one(cfg, seed, variant, timeout_s=180):
    req = {
        "entityCounts": cfg["entityCounts"],
        "constructs": cfg["constructs"],
        "seed": seed,
        "structure": {"hierarchyTargetDepth": cfg["structure"]["hierarchyTargetDepth"],
                      "hierarchyBranchingFactor": cfg["structure"]["hierarchyBranchingFactor"]},
        "variants": [variant],
        "targetProfile": "DL",
        "strictConsistency": True,
    }
    t0 = time.time()
    job = post("/api/generations", req)
    job_id = job["jobId"]
    while True:
        if time.time() - t0 > timeout_s:
            return {"status": "TIMEOUT", "elapsed_s": time.time() - t0}
        job = get(f"/api/generations/{job_id}")
        if job["status"] in ("COMPLETED", "FAILED"):
            break
        time.sleep(0.05)
    elapsed = time.time() - t0
    result = {"status": job["status"], "elapsed_s": elapsed}
    if job.get("variants"):
        v = job["variants"][0]
        result["verif_status"] = v.get("verification", {}).get("status")
        m = v.get("metrics", {})
        if m:
            result["depth"] = m.get("hierarchy", {}).get("maxDepth")
            result["branching"] = m.get("hierarchy", {}).get("avgBranchingFactor")
    return result

configs = json.load(open("reference_configs.json"))
results = {}
for ref in REFS:
    cfg = configs[ref]
    target_depth = cfg["structure"]["hierarchyTargetDepth"]
    target_branch = cfg["structure"]["hierarchyBranchingFactor"]
    for variant in VARIANTS:
        key = f"{ref}_{variant}"
        r = run_one(cfg, seed=2026, variant=variant)
        results[key] = r
        depth_str = f"depth={r.get('depth')}" if "depth" in r else ""
        print(f"{key:<28} target(depth={target_depth},branch={target_branch})  "
              f"status={r.get('verif_status', r['status']):<20} elapsed={r['elapsed_s']:.2f}s  "
              f"{depth_str} branch={r.get('branching')}", flush=True)
        time.sleep(1)  # brief pause between requests to let the JVM settle

with open("clean_spotcheck_results.json", "w") as f:
    json.dump(results, f, indent=2)
print("done")
