import json
import subprocess

REFS = ["prov-o", "ssn", "sosa", "pizza", "goslim_generic", "wine"]

FULL_CP = open("full_cp.txt").read().strip()

def load_json_ignoring_log_lines(path):
    text = open(path).read()
    # Java loggers write to the same stream in some environments; MetricsTool/ConstructCounter
    # print pure JSON to stdout, but be defensive and strip any leading non-JSON lines.
    start = text.index("{")
    return json.loads(text[start:])

def get_minimums(construct_names):
    if not construct_names:
        return {"classes": 0, "objectProperties": 0, "dataProperties": 0, "individuals": 0}
    arg = ",".join(construct_names)
    out = subprocess.run(
        ["java", "-cp", f"{FULL_CP};.", "MinimumsTool", arg],
        capture_output=True, text=True, check=True,
    )
    return json.loads(out.stdout.strip())

configs = {}
for ref in REFS:
    metrics = load_json_ignoring_log_lines(f"metrics-out/{ref}.json")
    counts = load_json_ignoring_log_lines(f"construct-counts/{ref}.json")

    nonzero_constructs = {k: v for k, v in counts.items() if v > 0}
    minimums = get_minimums(list(nonzero_constructs.keys()))

    observed_entities = {
        "classes": metrics["numClasses"],
        "objectProperties": metrics["numObjectProperties"],
        "dataProperties": metrics["numDataProperties"],
        "individuals": metrics["numIndividuals"],
    }
    final_entities = {k: max(observed_entities[k], minimums[k]) for k in observed_entities}
    bumped = {k: (observed_entities[k], final_entities[k]) for k in observed_entities if final_entities[k] != observed_entities[k]}

    hierarchy_depth = max(1, metrics["hierarchy"]["maxDepth"])
    branching = max(1, round(metrics["hierarchy"]["avgBranchingFactor"]))
    nesting_depth = min(10, metrics["nesting"]["maxDepth"])

    config = {
        "reference": ref,
        "entityCounts": final_entities,
        "observedEntityCounts": observed_entities,
        "minimumRequiredEntityCounts": minimums,
        "entityCountsBumpedFields": bumped,
        "constructs": nonzero_constructs,
        "totalRequestedAxioms": sum(nonzero_constructs.values()),
        "structure": {
            "hierarchyTargetDepth": hierarchy_depth,
            "hierarchyBranchingFactor": branching,
            "nestingMaxDepth": nesting_depth,
        },
        "structureObserved": {
            "hierarchyMaxDepth": metrics["hierarchy"]["maxDepth"],
            "hierarchyAvgBranchingFactor": metrics["hierarchy"]["avgBranchingFactor"],
            "nestingMaxDepth": metrics["nesting"]["maxDepth"],
        },
    }
    configs[ref] = config
    print(f"=== {ref} ===")
    print(f"  entities observed: {observed_entities}")
    if bumped:
        print(f"  entities BUMPED to satisfy construct minimums: {bumped}")
    else:
        print(f"  entities: no bump needed (observed already >= minimum)")
    print(f"  constructs requested ({len(nonzero_constructs)} types, {sum(nonzero_constructs.values())} total axioms): {nonzero_constructs}")
    print(f"  structure: {config['structure']} (observed depth={metrics['hierarchy']['maxDepth']}, branching={metrics['hierarchy']['avgBranchingFactor']:.2f})")
    print()

with open("reference_configs.json", "w") as f:
    json.dump(configs, f, indent=2)
print("Wrote reference_configs.json")
