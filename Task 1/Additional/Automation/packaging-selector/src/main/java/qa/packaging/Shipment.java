package qa.packaging;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * The shape of everything that has to go into one packaging.
 * <ul>
 *   <li>one item: the item itself;</li>
 *   <li>several items (stacked): length = max length, width = max width,
 *       thickness = sum of thicknesses, weight = sum of weights.</li>
 * </ul>
 */
public record Shipment(BigDecimal weight, BigDecimal length, BigDecimal width, BigDecimal thickness) {

    public static Shipment of(List<Item> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("shipment must contain at least one item");
        }
        BigDecimal weight = BigDecimal.ZERO;
        BigDecimal thickness = BigDecimal.ZERO;
        BigDecimal length = BigDecimal.ZERO;
        BigDecimal width = BigDecimal.ZERO;
        for (Item item : items) {
            weight = weight.add(item.weight());
            thickness = thickness.add(item.thickness());
            length = length.max(item.length());
            width = width.max(item.width());
        }
        return new Shipment(weight, length, width, thickness);
    }

    /** Dimensions sorted descending: [Max, Mid, Min]. */
    public List<BigDecimal> sortedDescending() {
        return List.of(length, width, thickness).stream()
                .sorted(Comparator.reverseOrder())
                .toList();
    }
}
