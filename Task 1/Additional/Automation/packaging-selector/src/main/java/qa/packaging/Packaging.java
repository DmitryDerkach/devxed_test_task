package qa.packaging;

import java.math.BigDecimal;
import java.util.List;

/**
 * A packaging option. The three internal dimensions are kept in the order they were given;
 * the fit rules sort them where needed.
 */
public record Packaging(String name,
                        BigDecimal cost,
                        boolean available,
                        PackagingType type,
                        BigDecimal maxWeight,
                        List<BigDecimal> dimensions) {

    public Packaging {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("packaging name must not be blank");
        }
        Checks.nonNegative(cost, "packaging cost");
        if (type == null) {
            throw new IllegalArgumentException("packaging type must be set");
        }
        Checks.positive(maxWeight, "packaging max weight");
        if (dimensions == null || dimensions.size() != 3) {
            throw new IllegalArgumentException("packaging must have exactly 3 dimensions");
        }
        dimensions = List.copyOf(dimensions);
        dimensions.forEach(d -> Checks.positive(d, "packaging dimension"));
    }
}
