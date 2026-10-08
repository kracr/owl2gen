import json
import subprocess
import sys
import time
import urllib.request

BASE = "http://localhost:8080"
JAR = r"D:\owl2gen-dl\backend\target\owl2gen-dl-backend-0.1.0-SNAPSHOT.jar"
JAVA = r"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\java.exe"
VARIANTS = ["UNIFORM_RANDOM", "CHAIN", "BALANCED_TREE", "PREFERENTIAL"]
ALL_CONSTRUCTS = json.load(open("all_constructs.json"))
RESULTS_FILE = "rigorous_basic_results.json"

def start_server():
    log = open("rigorous_basic_server.log", "a", encoding="utf-8")
    proc = subprocess.Popen([JAVA, "-jar", JAR], stdout=log, stderr=subprocess.STDOUT, cwd=r"D:\owl2gen-dl\backend")
    for _ in range(60):
        try:
            urllib.request.urlopen(BASE + "/api/catalog", timeout=2)
            return proc
        except Exception:
            time.sleep(1)
    raise RuntimeError("server did not become healthy in time")

def stop_server(proc):
    proc.terminate()
    try:
        proc.wait(timeout=10)
    except subprocess.TimeoutExpired:
        proc.kill()
        proc.wait(timeout=10)

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

def run_one(req, timeout_s=60):
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
        if time.time() - t0 > timeout_s + 60:
            return {"status": "CLIENT_GAVE_UP", "elapsed_s": time.time() - t0}
        time.sleep(0.1)
    elapsed = time.time() - t0
    v = job.get("variants", [{}])[0]
    entry = {"status": job["status"], "elapsed_s": elapsed, "axiomCount": v.get("axiomCount")}
    entry["verif_status"] = (v.get("verification") or {}).get("status")
    m = v.get("metrics") or {}
    if m:
        h = m.get("hierarchy", {})
        entry["hierarchy_depth"] = h.get("maxDepth")
        entry["branching"] = h.get("avgBranchingFactor")
        entry["tangledness"] = h.get("tangledness")
        entry["nesting_max_depth"] = m.get("nesting", {}).get("maxDepth")
        g = m.get("graph", {})
        entry["avg_degree"] = g.get("avgDegree")
        entry["clustering"] = g.get("clusteringCoefficient")
        entry["inferred_ratio"] = m.get("reasoning", {}).get("inferredToAssertedRatio")
    return entry

def main():
    try:
        results = json.load(open(RESULTS_FILE))
    except FileNotFoundError:
        results = {}

    for per_construct in (2, 5, 8):
        for variant in VARIANTS:
            for seed in range(1, 11):
                key = f"scaling_{per_construct}_{variant}_{seed}"
                if key in results:
                    continue
                proc = start_server()
                try:
                    req = build_request(per_construct, variant, seed)
                    entry = run_one(req)
                finally:
                    stop_server(proc)
                results[key] = entry
                print(f"{key}: {entry.get('verif_status', entry['status'])} in {entry['elapsed_s']:.2f}s", flush=True)
                json.dump(results, open(RESULTS_FILE, "w"), indent=2)

if __name__ == "__main__":
    main()
