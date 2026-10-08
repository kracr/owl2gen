import java.util.ArrayList;
import java.util.List;

import com.owl2gendl.catalog.ConstructId;
import com.owl2gendl.catalog.EntityRequirement;
import com.owl2gendl.catalog.MinimumEntityRequirements;

/**
 * Given a comma-separated list of ConstructId names (the observed, non-zero constructs from a reference
 * ontology, as produced by ConstructCounter), prints the minimum entity counts OWL2Gen-DL's own
 * RequestValidator requires for that exact selection -- reusing the real backend class, not a
 * reimplementation. Used to take a componentwise max() against a reference ontology's own observed entity
 * counts, so the derived request is guaranteed acceptable without inflating counts beyond what generating
 * the selected constructs structurally requires.
 */
public class MinimumsTool {
    public static void main(String[] args) {
        List<ConstructId> selected = new ArrayList<>();
        for (String name : args[0].split(",")) {
            if (name.isBlank()) continue;
            selected.add(ConstructId.valueOf(name.trim()));
        }
        EntityRequirement req = MinimumEntityRequirements.forSelection(selected);
        System.out.println("{\"classes\": " + req.classes() + ", \"objectProperties\": " + req.objectProperties()
                + ", \"dataProperties\": " + req.dataProperties() + ", \"individuals\": " + req.individuals() + "}");
    }
}
