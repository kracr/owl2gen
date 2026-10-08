import java.io.File;
import java.time.Duration;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.owl2gendl.metrics.EntityRelationGraphBuilder;
import com.owl2gendl.metrics.GraphMetrics;
import com.owl2gendl.metrics.GraphMetricsCalculator;
import com.owl2gendl.metrics.HierarchyAnalyzer;
import com.owl2gendl.metrics.HierarchyMetrics;
import com.owl2gendl.metrics.InferredVsAssertedCalculator;
import com.owl2gendl.metrics.NestingDepthAnalyzer;
import com.owl2gendl.metrics.NestingMetrics;
import com.owl2gendl.metrics.ReasoningMetrics;

/**
 * Loads an arbitrary real OWL file and runs it through OWL2Gen-DL's actual, unmodified metric classes
 * (HierarchyAnalyzer, EntityRelationGraphBuilder, GraphMetricsCalculator, InferredVsAssertedCalculator,
 * NestingDepthAnalyzer -- the same ones the live application uses on its own generated output), so real
 * reference ontologies can be measured with byte-identical methodology, not a reimplementation.
 */
public class MetricsTool {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: MetricsTool <path-to-owl-file> [reasoningTimeoutSeconds]");
            System.exit(1);
        }
        String path = args[0];
        int timeoutSec = args.length > 1 ? Integer.parseInt(args[1]) : 60;

        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(path));

        int numClasses = ontology.getClassesInSignature().size();
        int numObjProps = ontology.getObjectPropertiesInSignature().size();
        int numDataProps = ontology.getDataPropertiesInSignature().size();
        int numIndividuals = ontology.getIndividualsInSignature().size();
        int numAxioms = ontology.getAxiomCount();

        NestingDepthAnalyzer nestingAnalyzer = new NestingDepthAnalyzer();
        HierarchyAnalyzer hierarchyAnalyzer = new HierarchyAnalyzer();
        EntityRelationGraphBuilder graphBuilder = new EntityRelationGraphBuilder();
        GraphMetricsCalculator graphMetricsCalculator = new GraphMetricsCalculator();
        InferredVsAssertedCalculator inferredVsAssertedCalculator = new InferredVsAssertedCalculator();

        long t0 = System.currentTimeMillis();
        NestingMetrics nesting = nestingAnalyzer.analyze(ontology);
        HierarchyMetrics hierarchy = hierarchyAnalyzer.analyze(ontology);
        GraphMetrics graph = graphMetricsCalculator.analyze(graphBuilder.build(ontology));
        ReasoningMetrics reasoning = inferredVsAssertedCalculator.analyze(ontology, Duration.ofSeconds(timeoutSec));
        long elapsed = System.currentTimeMillis() - t0;

        System.out.println("{");
        System.out.println("  \"file\": \"" + path.replace("\\", "\\\\") + "\",");
        System.out.println("  \"numClasses\": " + numClasses + ",");
        System.out.println("  \"numObjectProperties\": " + numObjProps + ",");
        System.out.println("  \"numDataProperties\": " + numDataProps + ",");
        System.out.println("  \"numIndividuals\": " + numIndividuals + ",");
        System.out.println("  \"numAxioms\": " + numAxioms + ",");
        System.out.println("  \"metricsElapsedMs\": " + elapsed + ",");
        System.out.println("  \"nesting\": {\"maxDepth\": " + nesting.maxDepth() + ", \"avgDepth\": " + nesting.avgDepth() + "},");
        System.out.println("  \"hierarchy\": {\"maxDepth\": " + hierarchy.maxDepth() + ", \"avgBranchingFactor\": " + hierarchy.avgBranchingFactor() + ", \"tangledness\": " + hierarchy.tangledness() + "},");
        System.out.println("  \"graph\": {\"nodeCount\": " + graph.nodeCount() + ", \"edgeCount\": " + graph.edgeCount() + ", \"avgDegree\": " + graph.avgDegree() + ", \"clusteringCoefficient\": " + graph.clusteringCoefficient() + "},");
        System.out.println("  \"reasoning\": {\"computed\": " + reasoning.computed() + ", \"assertedSubClassOfCount\": " + reasoning.assertedSubClassOfCount() + ", \"inferredNewSubClassOfCount\": " + reasoning.inferredNewSubClassOfCount() + ", \"inferredToAssertedRatio\": " + reasoning.inferredToAssertedRatio() + "}");
        System.out.println("}");

        System.exit(0);
    }
}
