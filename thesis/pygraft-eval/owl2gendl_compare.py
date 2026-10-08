import json
import time
import sys
import urllib.request

BASE = "http://localhost:8080"

def post(path, payload):
    data = json.dumps(payload).encode()
    req = urllib.request.Request(BASE + path, data=data, headers={"Content-Type": "application/json"}, method="POST")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def get(path):
    with urllib.request.urlopen(BASE + path) as resp:
        return json.loads(resp.read())

VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]

# (classes, objProps, dataProps, individuals, hierarchyDepth, subClassOf, disjointWith,
#  classAssertion, objPropAssertion, dataPropAssertion) -- mirrors PyGraft's small/medium/large/mega
# schema+graph specs (num_classes/num_relations/max_hierarchy_depth/avg_disjointness*classes for the
# schema side, num_entities/num_triples split evenly across the three assertion axiom types for the
# graph side, since those serialize ~1:1 to RDF triples the same way PyGraft's do).
TIER_ARGS = {
    "small":  (25, 13, 12, 100, 3, 25, 5, 334, 333, 333),
    "medium": (100, 50, 50, 1000, 4, 100, 20, 3334, 3333, 3333),
    "large":  (250, 125, 125, 10000, 5, 250, 50, 33334, 33333, 33333),
    "mega":   (250, 125, 125, 100000, 5, 250, 50, 333334, 333333, 333333),
}

def build_request(classes, obj_props, data_props, individuals, hierarchy_depth,
                   sub_class_of, disjoint_with, class_assertion, obj_prop_assertion, data_prop_assertion,
                   seed=2026, variant="UNIFORM_RANDOM", strict=False):
    constructs = {
        "SUB_CLASS_OF": sub_class_of,
        "DISJOINT_WITH": disjoint_with,
        "CLASS_ASSERTION": class_assertion,
        "OBJECT_PROPERTY_ASSERTION": obj_prop_assertion,
        "DATA_PROPERTY_ASSERTION": data_prop_assertion,
    }
    return {
        "entityCounts": {
            "classes": classes,
            "objectProperties": obj_props,
            "dataProperties": data_props,
            "individuals": individuals,
        },
        "constructs": constructs,
        "seed": seed,
        "structure": {
            "hierarchyTargetDepth": hierarchy_depth,
            "hierarchyBranchingFactor": 3,
        },
        "variants": [variant],
        "targetProfile": "DL",
        "strictConsistency": strict,
    }

def run_one(name, req, timeout_s=1800):
    print(f"=== {name} ===", flush=True)
    total_requested = sum(req["constructs"].values())
    t0 = time.time()
    job = post("/api/generations", req)
    job_id = job["jobId"]
    while True:
        elapsed = time.time() - t0
        if elapsed > timeout_s:
            print(f"  TIMEOUT after {elapsed:.1f}s", flush=True)
            return {"name": name, "status": "TIMEOUT", "elapsed_s": elapsed, "requested_axioms": total_requested}
        job = get(f"/api/generations/{job_id}")
        if job["status"] in ("COMPLETED", "FAILED"):
            break
        time.sleep(0.05)
    elapsed = time.time() - t0
    result = {"name": name, "status": job["status"], "elapsed_s": elapsed, "requested_axioms": total_requested}
    if job.get("error"):
        result["error"] = job["error"]
    if job.get("variants"):
        v = job["variants"][0]
        result["actual_axioms"] = v.get("axiomCount")
        result["variant_status"] = v.get("status")
        result["verification"] = v.get("verification") or {}
        result["metrics"] = v.get("metrics") or {}
    print(f"  {result.get('status')} in {elapsed:.3f}s, actual_axioms={result.get('actual_axioms')}, "
          f"verification={(result.get('verification') or {}).get('status')}", flush=True)
    return result

def warmup():
    # absorb JVM/JIT cold-start cost on a throwaway tiny request before any timed run
    req = build_request(5, 3, 2, 10, 2, 5, 1, 10, 10, 10)
    run_one("warmup", req, timeout_s=60)

def load_results(path):
    try:
        with open(path, "r") as f:
            return json.load(f)
    except FileNotFoundError:
        return []

def save_result(path, result):
    existing = load_results(path)
    existing = [r for r in existing if r["name"] != result["name"]]
    existing.append(result)
    with open(path, "w") as f:
        json.dump(existing, f, indent=2, default=str)

if __name__ == "__main__":
    mode = sys.argv[1] if len(sys.argv) > 1 else "matrix"
    out_path = "owl2gendl_results.json"

    if mode == "matrix":
        # usage: python owl2gendl_compare.py matrix <tier> [strict]
        tier = sys.argv[2] if len(sys.argv) > 2 else "small"
        strict = len(sys.argv) > 3 and sys.argv[3] == "strict"
        warmup()
        for variant in VARIANTS:
            args = TIER_ARGS[tier]
            req = build_request(*args, variant=variant, strict=strict)
            name = f"{tier}_{variant.lower()}" + ("_strict" if strict else "")
            result = run_one(name, req, timeout_s=1800)
            save_result(out_path, result)
    else:
        # single named run: python owl2gendl_compare.py <tier> [variant] [strict]
        tier = mode
        variant = sys.argv[2] if len(sys.argv) > 2 else "UNIFORM_RANDOM"
        strict = len(sys.argv) > 3 and sys.argv[3] == "strict"
        args = TIER_ARGS[tier]
        req = build_request(*args, variant=variant, strict=strict)
        name = f"{tier}_{variant.lower()}" + ("_strict" if strict else "")
        result = run_one(name, req, timeout_s=1800)
        save_result(out_path, result)
