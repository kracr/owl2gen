import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

/**
 * Counts real axioms/class-expressions in an arbitrary loaded ontology, mapped onto OWL2Gen-DL's own
 * 64-construct catalogue (com.owl2gendl.catalog.ConstructId), so a real reference ontology's *observable*
 * construct usage can be used to derive an OWL2Gen request -- entity counts and per-construct-type counts
 * only, never the emergent structural metrics MetricsTool reports, to avoid circularity.
 */
public class ConstructCounter {

    enum C {
        OBJECT_COMPLEMENT_OF, OBJECT_INTERSECTION_OF, OBJECT_ONE_OF, OBJECT_UNION_OF,
        DISJOINT_UNION, DISJOINT_WITH, EQUIVALENT_CLASSES, SUB_CLASS_OF, ALL_DISJOINT_CLASSES,
        ASYMMETRIC_PROPERTY, EQUIVALENT_OBJECT_PROPERTY, FUNCTIONAL_OBJECT_PROPERTY, INVERSE_FUNCTIONAL_PROPERTY,
        INVERSE_OF_PROPERTY, IRREFLEXIVE_PROPERTY, OBJECT_PROPERTY_DISJOINT_WITH, REFLEXIVE_PROPERTY,
        SYMMETRIC_PROPERTY, TRANSITIVE_PROPERTY, OBJECT_PROPERTY_DOMAIN, OBJECT_PROPERTY_RANGE,
        PROPERTY_CHAIN_AXIOM, SUB_OBJECT_PROPERTY_OF, ALL_DISJOINT_OBJECT_PROPERTIES,
        OBJECT_ALL_VALUES_FROM, OBJECT_HAS_SELF, OBJECT_HAS_VALUE, OBJECT_SOME_VALUES_FROM,
        OBJECT_MIN_CARDINALITY, OBJECT_MAX_CARDINALITY, OBJECT_EXACT_CARDINALITY,
        OBJECT_MIN_QUALIFIED_CARDINALITY, OBJECT_MAX_QUALIFIED_CARDINALITY, OBJECT_QUALIFIED_CARDINALITY,
        EQUIVALENT_DATA_PROPERTY, FUNCTIONAL_DATA_PROPERTY, DATA_PROPERTY_DISJOINT_WITH,
        DATA_PROPERTY_DOMAIN, DATA_PROPERTY_RANGE, SUB_DATA_PROPERTY_OF, ALL_DISJOINT_DATA_PROPERTIES,
        DATA_ALL_VALUES_FROM, DATA_HAS_VALUE, DATA_SOME_VALUES_FROM,
        DATA_MIN_CARDINALITY, DATA_MAX_CARDINALITY, DATA_EXACT_CARDINALITY,
        DATA_MIN_QUALIFIED_CARDINALITY, DATA_MAX_QUALIFIED_CARDINALITY, DATA_QUALIFIED_CARDINALITY,
        HAS_KEY, CLASS_ASSERTION, OBJECT_PROPERTY_ASSERTION, DATA_PROPERTY_ASSERTION,
        SAME_INDIVIDUAL, DIFFERENT_INDIVIDUALS, NEGATIVE_OBJECT_PROPERTY_ASSERTION, NEGATIVE_DATA_PROPERTY_ASSERTION
    }

    public static void main(String[] args) throws Exception {
        String path = args[0];
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology o = manager.loadOntologyFromOntologyDocument(new File(path));

        Map<C, Integer> counts = new EnumMap<>(C.class);
        for (C c : C.values()) counts.put(c, 0);

        // --- axiom-type-level constructs ---
        inc(counts, C.DISJOINT_UNION, o.getAxiomCount(AxiomType.DISJOINT_UNION));
        inc(counts, C.DISJOINT_WITH, o.getAxiomCount(AxiomType.DISJOINT_CLASSES)); // pairwise use counted separately below if needed
        inc(counts, C.EQUIVALENT_CLASSES, o.getAxiomCount(AxiomType.EQUIVALENT_CLASSES));
        inc(counts, C.SUB_CLASS_OF, o.getAxiomCount(AxiomType.SUBCLASS_OF));
        inc(counts, C.ASYMMETRIC_PROPERTY, o.getAxiomCount(AxiomType.ASYMMETRIC_OBJECT_PROPERTY));
        inc(counts, C.EQUIVALENT_OBJECT_PROPERTY, o.getAxiomCount(AxiomType.EQUIVALENT_OBJECT_PROPERTIES));
        inc(counts, C.FUNCTIONAL_OBJECT_PROPERTY, o.getAxiomCount(AxiomType.FUNCTIONAL_OBJECT_PROPERTY));
        inc(counts, C.INVERSE_FUNCTIONAL_PROPERTY, o.getAxiomCount(AxiomType.INVERSE_FUNCTIONAL_OBJECT_PROPERTY));
        inc(counts, C.INVERSE_OF_PROPERTY, o.getAxiomCount(AxiomType.INVERSE_OBJECT_PROPERTIES));
        inc(counts, C.IRREFLEXIVE_PROPERTY, o.getAxiomCount(AxiomType.IRREFLEXIVE_OBJECT_PROPERTY));
        inc(counts, C.OBJECT_PROPERTY_DISJOINT_WITH, o.getAxiomCount(AxiomType.DISJOINT_OBJECT_PROPERTIES));
        inc(counts, C.REFLEXIVE_PROPERTY, o.getAxiomCount(AxiomType.REFLEXIVE_OBJECT_PROPERTY));
        inc(counts, C.SYMMETRIC_PROPERTY, o.getAxiomCount(AxiomType.SYMMETRIC_OBJECT_PROPERTY));
        inc(counts, C.TRANSITIVE_PROPERTY, o.getAxiomCount(AxiomType.TRANSITIVE_OBJECT_PROPERTY));
        inc(counts, C.OBJECT_PROPERTY_DOMAIN, o.getAxiomCount(AxiomType.OBJECT_PROPERTY_DOMAIN));
        inc(counts, C.OBJECT_PROPERTY_RANGE, o.getAxiomCount(AxiomType.OBJECT_PROPERTY_RANGE));
        inc(counts, C.PROPERTY_CHAIN_AXIOM, o.getAxiomCount(AxiomType.SUB_PROPERTY_CHAIN_OF));
        inc(counts, C.SUB_OBJECT_PROPERTY_OF, o.getAxiomCount(AxiomType.SUB_OBJECT_PROPERTY));
        inc(counts, C.ALL_DISJOINT_OBJECT_PROPERTIES, 0); // OWL API folds into DISJOINT_OBJECT_PROPERTIES above
        inc(counts, C.EQUIVALENT_DATA_PROPERTY, o.getAxiomCount(AxiomType.EQUIVALENT_DATA_PROPERTIES));
        inc(counts, C.FUNCTIONAL_DATA_PROPERTY, o.getAxiomCount(AxiomType.FUNCTIONAL_DATA_PROPERTY));
        inc(counts, C.DATA_PROPERTY_DISJOINT_WITH, o.getAxiomCount(AxiomType.DISJOINT_DATA_PROPERTIES));
        inc(counts, C.DATA_PROPERTY_DOMAIN, o.getAxiomCount(AxiomType.DATA_PROPERTY_DOMAIN));
        inc(counts, C.DATA_PROPERTY_RANGE, o.getAxiomCount(AxiomType.DATA_PROPERTY_RANGE));
        inc(counts, C.SUB_DATA_PROPERTY_OF, o.getAxiomCount(AxiomType.SUB_DATA_PROPERTY));
        inc(counts, C.ALL_DISJOINT_DATA_PROPERTIES, 0);
        inc(counts, C.HAS_KEY, o.getAxiomCount(AxiomType.HAS_KEY));
        inc(counts, C.CLASS_ASSERTION, o.getAxiomCount(AxiomType.CLASS_ASSERTION));
        inc(counts, C.OBJECT_PROPERTY_ASSERTION, o.getAxiomCount(AxiomType.OBJECT_PROPERTY_ASSERTION));
        inc(counts, C.DATA_PROPERTY_ASSERTION, o.getAxiomCount(AxiomType.DATA_PROPERTY_ASSERTION));
        inc(counts, C.SAME_INDIVIDUAL, o.getAxiomCount(AxiomType.SAME_INDIVIDUAL));
        inc(counts, C.DIFFERENT_INDIVIDUALS, o.getAxiomCount(AxiomType.DIFFERENT_INDIVIDUALS));
        inc(counts, C.NEGATIVE_OBJECT_PROPERTY_ASSERTION, o.getAxiomCount(AxiomType.NEGATIVE_OBJECT_PROPERTY_ASSERTION));
        inc(counts, C.NEGATIVE_DATA_PROPERTY_ASSERTION, o.getAxiomCount(AxiomType.NEGATIVE_DATA_PROPERTY_ASSERTION));

        // ALL_DISJOINT_CLASSES vs DISJOINT_WITH: OWL API represents both n-ary and pairwise disjointness as
        // OWLDisjointClassesAxiom; split by operand count (>2 = genuinely n-ary, else pairwise).
        int pairwiseDisjoint = 0, naryDisjoint = 0;
        for (OWLDisjointClassesAxiom ax : o.getAxioms(AxiomType.DISJOINT_CLASSES)) {
            if (ax.getClassExpressions().size() > 2) naryDisjoint++; else pairwiseDisjoint++;
        }
        counts.put(C.DISJOINT_WITH, pairwiseDisjoint);
        counts.put(C.ALL_DISJOINT_CLASSES, naryDisjoint);

        // --- class-expression-level constructs: walk every nested class expression across all axioms ---
        for (OWLAxiom axiom : o.getAxioms()) {
            for (OWLClassExpression ce : axiom.getNestedClassExpressions()) {
                classify(ce, counts);
            }
        }

        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (C c : C.values()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("  \"").append(c.name()).append("\": ").append(counts.get(c));
        }
        sb.append("\n}");
        System.out.println(sb);
    }

    private static void classify(OWLClassExpression ce, Map<C, Integer> counts) {
        if (ce instanceof OWLObjectComplementOf) inc(counts, C.OBJECT_COMPLEMENT_OF, 1);
        else if (ce instanceof OWLObjectIntersectionOf) inc(counts, C.OBJECT_INTERSECTION_OF, 1);
        else if (ce instanceof OWLObjectUnionOf) inc(counts, C.OBJECT_UNION_OF, 1);
        else if (ce instanceof OWLObjectOneOf) inc(counts, C.OBJECT_ONE_OF, 1);
        else if (ce instanceof OWLObjectAllValuesFrom) inc(counts, C.OBJECT_ALL_VALUES_FROM, 1);
        else if (ce instanceof OWLObjectHasSelf) inc(counts, C.OBJECT_HAS_SELF, 1);
        else if (ce instanceof OWLObjectHasValue) inc(counts, C.OBJECT_HAS_VALUE, 1);
        else if (ce instanceof OWLObjectSomeValuesFrom) inc(counts, C.OBJECT_SOME_VALUES_FROM, 1);
        else if (ce instanceof OWLObjectMinCardinality m) inc(counts, m.isQualified() ? C.OBJECT_MIN_QUALIFIED_CARDINALITY : C.OBJECT_MIN_CARDINALITY, 1);
        else if (ce instanceof OWLObjectMaxCardinality m) inc(counts, m.isQualified() ? C.OBJECT_MAX_QUALIFIED_CARDINALITY : C.OBJECT_MAX_CARDINALITY, 1);
        else if (ce instanceof OWLObjectExactCardinality m) inc(counts, m.isQualified() ? C.OBJECT_QUALIFIED_CARDINALITY : C.OBJECT_EXACT_CARDINALITY, 1);
        else if (ce instanceof OWLDataAllValuesFrom) inc(counts, C.DATA_ALL_VALUES_FROM, 1);
        else if (ce instanceof OWLDataHasValue) inc(counts, C.DATA_HAS_VALUE, 1);
        else if (ce instanceof OWLDataSomeValuesFrom) inc(counts, C.DATA_SOME_VALUES_FROM, 1);
        else if (ce instanceof OWLDataMinCardinality m) inc(counts, m.isQualified() ? C.DATA_MIN_QUALIFIED_CARDINALITY : C.DATA_MIN_CARDINALITY, 1);
        else if (ce instanceof OWLDataMaxCardinality m) inc(counts, m.isQualified() ? C.DATA_MAX_QUALIFIED_CARDINALITY : C.DATA_MAX_CARDINALITY, 1);
        else if (ce instanceof OWLDataExactCardinality m) inc(counts, m.isQualified() ? C.DATA_QUALIFIED_CARDINALITY : C.DATA_EXACT_CARDINALITY, 1);
    }

    private static void inc(Map<C, Integer> counts, C c, int n) {
        counts.merge(c, n, Integer::sum);
    }
}
