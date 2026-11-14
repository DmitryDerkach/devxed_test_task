package qa.packaging;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Suggests the cheapest available packaging the shipment fits into.
 * Equal price is resolved by packaging name (case-insensitive, ascending) so the result is deterministic.
 */
public final class PackagingSelector {

    public static final Comparator<Packaging> CHEAPEST_FIRST =
            Comparator.comparing(Packaging::cost)
                    .thenComparing(Packaging::name, String.CASE_INSENSITIVE_ORDER);

    private final ShipmentShaper shaper;
    private final FitRules rules;
    private final Comparator<Packaging> preference;

    public PackagingSelector() {
        this(ShipmentShaper.STANDARD, FitRules.STANDARD, CHEAPEST_FIRST);
    }

    public PackagingSelector(ShipmentShaper shaper, FitRules rules, Comparator<Packaging> preference) {
        this.shaper = shaper;
        this.rules = rules;
        this.preference = preference;
    }

    /**
     * @return the cheapest suitable packaging, or empty when nothing suits the shipment
     * @throws IllegalArgumentException when the item list is empty
     */
    public Optional<Packaging> select(List<Item> items, List<Packaging> catalog) {
        Shipment shipment = shaper.shape(items);
        return catalog.stream()
                .filter(packaging -> rules.isSuitable(shipment, packaging))
                .min(preference);
    }
}
