const CONSTRUCT_IDS = [
  "OBJECT_COMPLEMENT_OF","OBJECT_INTERSECTION_OF","OBJECT_ONE_OF","OBJECT_UNION_OF",
  "DISJOINT_UNION","DISJOINT_WITH","EQUIVALENT_CLASSES","SUB_CLASS_OF","ALL_DISJOINT_CLASSES",
  "ASYMMETRIC_PROPERTY","EQUIVALENT_OBJECT_PROPERTY","FUNCTIONAL_OBJECT_PROPERTY",
  "INVERSE_FUNCTIONAL_PROPERTY","INVERSE_OF_PROPERTY","IRREFLEXIVE_PROPERTY",
  "OBJECT_PROPERTY_DISJOINT_WITH","REFLEXIVE_PROPERTY","SYMMETRIC_PROPERTY","TRANSITIVE_PROPERTY",
  "OBJECT_PROPERTY_DOMAIN","OBJECT_PROPERTY_RANGE","PROPERTY_CHAIN_AXIOM","SUB_OBJECT_PROPERTY_OF",
  "ALL_DISJOINT_OBJECT_PROPERTIES",
  "OBJECT_ALL_VALUES_FROM","OBJECT_HAS_SELF","OBJECT_HAS_VALUE","OBJECT_SOME_VALUES_FROM",
  "OBJECT_MIN_CARDINALITY","OBJECT_MAX_CARDINALITY","OBJECT_EXACT_CARDINALITY",
  "OBJECT_MIN_QUALIFIED_CARDINALITY","OBJECT_MAX_QUALIFIED_CARDINALITY","OBJECT_QUALIFIED_CARDINALITY",
  "EQUIVALENT_DATA_PROPERTY","FUNCTIONAL_DATA_PROPERTY","DATA_PROPERTY_DISJOINT_WITH",
  "DATA_PROPERTY_DOMAIN","DATA_PROPERTY_RANGE","SUB_DATA_PROPERTY_OF","ALL_DISJOINT_DATA_PROPERTIES",
  "DATA_ALL_VALUES_FROM","DATA_HAS_VALUE","DATA_SOME_VALUES_FROM","DATA_MIN_CARDINALITY",
  "DATA_MAX_CARDINALITY","DATA_EXACT_CARDINALITY","DATA_MIN_QUALIFIED_CARDINALITY",
  "DATA_MAX_QUALIFIED_CARDINALITY","DATA_QUALIFIED_CARDINALITY",
  "DATA_COMPLEMENT_OF","DATA_INTERSECTION_OF","DATA_ONE_OF","DATA_UNION_OF",
  "DATATYPE_DEFINITION","DATATYPE_RESTRICTION",
  "HAS_KEY","CLASS_ASSERTION","OBJECT_PROPERTY_ASSERTION","DATA_PROPERTY_ASSERTION",
  "SAME_INDIVIDUAL","DIFFERENT_INDIVIDUALS","NEGATIVE_OBJECT_PROPERTY_ASSERTION",
  "NEGATIVE_DATA_PROPERTY_ASSERTION"
];

const BASE_URL = "http://localhost:8080";

function buildRequest({ seed, variant, countPerConstruct, tier }) {
  const constructs = {};
  for (const id of CONSTRUCT_IDS) constructs[id] = countPerConstruct;
  return {
    entityCounts: { classes: 80, objectProperties: 6, dataProperties: 3, individuals: 10 },
    constructs,
    seed,
    structure: { hierarchyTargetDepth: 5, hierarchyBranchingFactor: 3, nestingMaxDepth: 2, nestingProbability: 0.3 },
    variants: [variant],
    reasoning: tier ? { tier } : null,
    targetProfile: "DL",
    strictConsistency: true
  };
}

async function submit(request) {
  const res = await fetch(`${BASE_URL}/api/generations`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(request)
  });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`submit failed: ${res.status} ${text}`);
  }
  return res.json();
}

async function poll(jobId, maxWaitMs, intervalMs) {
  const start = Date.now();
  while (Date.now() - start < maxWaitMs) {
    const res = await fetch(`${BASE_URL}/api/generations/${jobId}`);
    const job = await res.json();
    if (job.status === "COMPLETED" || job.status === "FAILED") {
      return { job, elapsedMs: Date.now() - start };
    }
    await new Promise(r => setTimeout(r, intervalMs));
  }
  return { job: null, elapsedMs: Date.now() - start, timedOut: true };
}

async function runOne(label, requestParams, maxWaitMs) {
  const t0 = Date.now();
  const request = buildRequest(requestParams);
  const submitted = await submit(request);
  console.log(`[${label}] submitted job ${submitted.jobId}`);
  const { job, elapsedMs, timedOut } = await poll(submitted.jobId, maxWaitMs, 2000);
  const totalMs = Date.now() - t0;
  if (timedOut) {
    console.log(`[${label}] job ${submitted.jobId} DID NOT COMPLETE within ${maxWaitMs}ms client-side poll (server may still be running it)`);
    return { label, jobId: submitted.jobId, timedOut: true, totalMs };
  }
  const variant = job.variants[0];
  console.log(`[${label}] job ${submitted.jobId} -> status=${job.status} variant.status=${variant.status} axioms=${variant.axiomCount} verification=${JSON.stringify(variant.verification)} totalMs=${totalMs}`);
  return { label, jobId: submitted.jobId, job, totalMs };
}

async function main() {
  const results = [];

  // Round 3: matched design - all four topologies at the identical 512 occurrences (8 per construct,
  // matching the existing thesis tables) and the identical three seeds, so topology is the only variable
  // that differs between cases. Supersedes round 1/2's Chain@640 runs for topology comparison purposes -
  // those used 640 occurrences for Chain only, confounding topology with workload size.
  const topologies = ["CHAIN", "UNIFORM_RANDOM", "BALANCED_TREE", "PREFERENTIAL"];
  const seeds = [1, 2, 3];
  for (const variant of topologies) {
    for (const seed of seeds) {
      const label = `${variant}@512/seed=${seed}`;
      results.push(await runOne(label, { seed, variant, countPerConstruct: 8 }, 15 * 60 * 1000));
    }
  }

  console.log("\n=== SUMMARY ===");
  for (const r of results) {
    console.log(JSON.stringify({ label: r.label, jobId: r.jobId, timedOut: !!r.timedOut, totalMs: r.totalMs }));
  }
}

main().catch(e => { console.error(e); process.exit(1); });
