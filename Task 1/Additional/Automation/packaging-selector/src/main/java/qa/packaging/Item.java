package qa.packaging;

import java.math.BigDecimal;

/**
 * A single unit in the shipment. Weight in kg, dimensions in cm.
 * Dimensions are NOT required to be ordered (width may exceed length).
 */
public record Item(BigDecimal weight, BigDecimal length, BigDecimal width, BigDecimal thickness) {

    public Item {
        Checks.positive(weight, "item weight");
        Checks.positive(length, "item length");
        Checks.positive(width, "item width");
        Checks.positive(thickness, "item thickness");
    }
}
