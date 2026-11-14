package qa.packaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Turns a {@link Scenario} into domain objects, runs a selector and reports a comparable outcome string. */
public final class ScenarioRunner {

    private ScenarioRunner() {
    }

    /** @return {@code PACKAGING:<name>}, {@code NONE}, {@code INVALID_INPUT} or {@code CRASH:<exception>} */
    public static String outcomeOf(PackagingSelector selector, Scenario scenario) {
        try {
            List<Item> items = new ArrayList<>();
            for (Scenario.ItemSpec spec : scenario.items()) {
                for (int i = 0; i < spec.times(); i++) {
                    items.add(new Item(spec.weight(), spec.length(), spec.width(), spec.thickness()));
                }
            }
            List<Packaging> catalog = scenario.packagings().stream()
                    .map(p -> new Packaging(p.name(), p.cost(), p.available(), p.type(), p.maxWeight(), p.dimensions()))
                    .toList();
            Optional<Packaging> result = selector.select(items, catalog);
            return result.map(p -> "PACKAGING:" + p.name()).orElse("NONE");
        } catch (IllegalArgumentException e) {
            return "INVALID_INPUT";
        } catch (RuntimeException e) {
            // Any other exception is a defect, never an accepted outcome: it can't equal an expected value.
            return "CRASH:" + e.getClass().getSimpleName();
        }
    }

    public static boolean passes(PackagingSelector selector, Scenario scenario) {
        return outcomeOf(selector, scenario).equals(scenario.expected().asText());
    }
}
