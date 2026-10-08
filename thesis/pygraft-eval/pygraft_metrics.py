"""
Computes the same structural metrics OWL2Gen-DL's backend computes (see
backend/src/main/java/com/owl2gendl/metrics/{HierarchyAnalyzer,EntityRelationGraphBuilder,
GraphMetricsCalculator,InferredVsAssertedCalculator}.java), applied to PyGraft's own real RDF output,
so the two tools' generated artifacts can be compared on the same footing, not just on timing.

Two deliberate normalisations, both documented here rather than hidden:
1. PyGraft explicitly asserts "X rdfs:subClassOf owl:Thing" for every root class; OWL2Gen-DL's own
   generator never asserts this (roots simply have no SubClassOf axiom at all). Counting Thing as a real
   parent would inflate every PyGraft root's depth by 1 and count every root as "tangled-free" for a
   reason OWL2Gen-DL's own roots never get credited for. owl:Thing is excluded from the hierarchy
   entirely, matching what an OWL2Gen-DL-generated root actually looks like.
2. The comparison OWL2Gen-DL requests used only SUB_CLASS_OF, DISJOINT_WITH, CLASS_ASSERTION,
   OBJECT_PROPERTY_ASSERTION, DATA_PROPERTY_ASSERTION -- no domain/range, inverseOf, or property
   characteristic axioms. PyGraft's schema always includes domain/range and property-characteristic
   triples regardless. Those are excluded from the co-occurrence graph here too, so both sides'
   graphs are built from the same *kinds* of axioms, not from whatever each tool happened to include.
"""
import sys
import json
from collections import defaultdict
import rdflib

OWL = rdflib.Namespace("http://www.w3.org/2002/07/owl#")
RDFS = rdflib.RDFS
RDF = rdflib.RDF
THING = OWL.Thing

def load_graph(schema_path, kg_path=None):
    g = rdflib.Graph()
    g.parse(schema_path, format="xml")
    if kg_path:
        g.parse(kg_path, format="xml")
    return g

def hierarchy_metrics(g):
    parents_of = defaultdict(set)
    for s, p, o in g.triples((None, RDFS.subClassOf, None)):
        if o == THING:
            continue  # normalisation (1)
        parents_of[s].add(o)

    if not parents_of:
        return {"maxDepth": 0, "avgBranchingFactor": 0.0, "tangledness": 0.0}

    depth_cache = {}
    def depth_of(cls, in_progress):
        if cls in depth_cache:
            return depth_cache[cls]
        parents = parents_of.get(cls)
        if not parents or cls in in_progress:
            return 0
        in_progress.add(cls)
        d = 1 + max(depth_of(p, in_progress) for p in parents)
        in_progress.discard(cls)
        depth_cache[cls] = d
        return d

    max_depth = max(depth_of(c, set()) for c in parents_of)

    child_count = defaultdict(int)
    for parents in parents_of.values():
        for parent in parents:
            child_count[parent] += 1
    avg_branching = sum(child_count.values()) / len(child_count) if child_count else 0.0

    tangled = sum(1 for parents in parents_of.values() if len(parents) > 1)
    tangledness = tangled / len(parents_of)

    return {"maxDepth": max_depth, "avgBranchingFactor": avg_branching, "tangledness": tangledness}

def is_declaration_or_schema_only(p, o):
    # Matches what OWL2Gen-DL's request never included: domain/range, inverseOf, property
    # characteristic type declarations (Symmetric/Transitive/Reflexive/Functional/...), and pure
    # rdf:type declarations of the entity itself being a Class/ObjectProperty/etc.
    if p == RDFS.domain or p == RDFS.range or p == OWL.inverseOf:
        return True
    if p == RDF.type and o in (OWL.Class, OWL.ObjectProperty, OWL.DatatypeProperty, OWL.NamedIndividual,
                                 OWL.SymmetricProperty, OWL.TransitiveProperty, OWL.ReflexiveProperty,
                                 OWL.IrreflexiveProperty, OWL.AsymmetricProperty, OWL.FunctionalProperty,
                                 OWL.InverseFunctionalProperty):
        return True
    return False

def graph_metrics(g, class_uris):
    adjacency = defaultdict(set)

    def connect(a, b):
        if a == b:
            return
        adjacency[a].add(b)
        adjacency[b].add(a)
        adjacency.setdefault(a, adjacency[a])
        adjacency.setdefault(b, adjacency[b])

    for s, p, o in g:
        if is_declaration_or_schema_only(p, o):
            continue
        if p == RDF.type:
            if o == THING or o not in class_uris:
                continue
            connect(s, o)  # class assertion: individual -> class
        elif p == RDFS.subClassOf:
            if o == THING:
                continue
            connect(s, o)
        elif p == OWL.disjointWith:
            connect(s, o)
        elif isinstance(o, rdflib.URIRef) and p not in (RDFS.subClassOf, RDF.type):
            # object-property assertion triple (s, relation, o): axiom-signature style, 3 pairwise edges
            connect(s, p)
            connect(s, o)
            connect(p, o)
        # datatype/literal-valued triples contribute no edges (a literal isn't a named entity either)

    # ensure isolated nodes referenced only via skipped triples aren't phantom-counted
    node_count = len(adjacency)
    if node_count == 0:
        return {"nodeCount": 0, "edgeCount": 0, "avgDegree": 0.0, "clusteringCoefficient": 0.0}

    edge_count = sum(len(v) for v in adjacency.values()) // 2
    avg_degree = (2.0 * edge_count) / node_count

    total = 0.0
    for neighbors in adjacency.values():
        degree = len(neighbors)
        if degree < 2:
            continue
        neighbor_list = list(neighbors)
        links = 0
        for i in range(len(neighbor_list)):
            nn = adjacency[neighbor_list[i]]
            for j in range(i + 1, len(neighbor_list)):
                if neighbor_list[j] in nn:
                    links += 1
        possible = degree * (degree - 1) / 2.0
        total += links / possible
    clustering = total / node_count

    return {"nodeCount": node_count, "edgeCount": edge_count, "avgDegree": avg_degree, "clusteringCoefficient": clustering}

def reasoning_metrics(g):
    parents_of = defaultdict(set)
    for s, p, o in g.triples((None, RDFS.subClassOf, None)):
        if o == THING:
            continue
        parents_of[s].add(o)

    asserted_count = sum(len(v) for v in parents_of.values())
    if asserted_count == 0:
        return {"computed": True, "assertedSubClassOfCount": 0, "inferredNewSubClassOfCount": 0, "inferredToAssertedRatio": 0.0}

    ancestor_cache = {}
    def ancestors_of(cls, in_progress):
        if cls in ancestor_cache:
            return ancestor_cache[cls]
        result = set()
        if cls in in_progress:
            return result
        in_progress.add(cls)
        for parent in parents_of.get(cls, ()):
            result.add(parent)
            result |= ancestors_of(parent, in_progress)
        in_progress.discard(cls)
        ancestor_cache[cls] = result
        return result

    inferred_new = 0
    for cls in parents_of:
        full_ancestors = ancestors_of(cls, set())
        direct_parents = parents_of[cls]
        inferred_new += len(full_ancestors - direct_parents)

    ratio = inferred_new / asserted_count
    return {"computed": True, "assertedSubClassOfCount": asserted_count, "inferredNewSubClassOfCount": inferred_new, "inferredToAssertedRatio": ratio}

def analyze(schema_path, kg_path, label):
    g = load_graph(schema_path, kg_path)
    class_uris = {s for s, p, o in g.triples((None, RDF.type, OWL.Class))}

    result = {
        "name": label,
        "triple_count": len(g),
        "hierarchy": hierarchy_metrics(g),
        "graph": graph_metrics(g, class_uris),
        "reasoning": reasoning_metrics(g),
    }
    print(json.dumps(result, indent=2))
    return result

if __name__ == "__main__":
    tier = sys.argv[1] if len(sys.argv) > 1 else "small"
    base = f"D:/owl2gen-dl/thesis/pygraft-eval/output/{tier}"
    schema_path = f"{base}/schema.rdf"
    kg_path = f"{base}/full_graph.rdf"
    result = analyze(schema_path, kg_path, tier)

    out_file = "pygraft_metrics_results.json"
    try:
        with open(out_file) as f:
            existing = json.load(f)
    except FileNotFoundError:
        existing = []
    existing = [r for r in existing if r["name"] != tier]
    existing.append(result)
    with open(out_file, "w") as f:
        json.dump(existing, f, indent=2)
