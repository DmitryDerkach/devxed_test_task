package qa.packaging;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One data-driven test scenario, loaded from {@code scenarios.json}.
 * The same file feeds the test-case spreadsheet, so documented and executed expectations cannot drift apart.
 */
public record Scenario(String id,
                       String group,
                       List<String> ref,
                       String technique,
                       String priority,
                       Map<String, String> title,
                       List<ItemSpec> items,
                       List<PackagingSpec> packagings,
                       Expected expected) {

    public record ItemSpec(BigDecimal weight, BigDecimal length, BigDecimal width, BigDecimal thickness,
                           Integer repeat) {
        public int times() {
            return repeat == null ? 1 : repeat;
        }
    }

    public record PackagingSpec(String name, BigDecimal cost, boolean available, PackagingType type,
                                BigDecimal maxWeight, List<BigDecimal> dimensions) {
    }

    /** outcome: PACKAGING (then {@code packaging} holds the name), NONE or INVALID_INPUT. */
    public record Expected(String outcome, String packaging) {
        public String asText() {
            return "PACKAGING".equals(outcome) ? "PACKAGING:" + packaging : outcome;
        }
    }

    @Override
    public String toString() {
        return id + " " + title.get("en");
    }
}
