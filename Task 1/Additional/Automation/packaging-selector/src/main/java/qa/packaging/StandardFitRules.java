package qa.packaging;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * The rules from the specification. Every individual rule is a small protected method so that
 * tests can derive deliberately broken variants (mutants) and prove the test-suite notices.
 */
public class StandardFitRules implements FitRules {

    @Override
    public boolean isSuitable(Shipment shipment, Packaging packaging) {
        return packaging.available()
                && withinMaxWeight(shipment, packaging)
                && fitsDimensions(shipment, packaging);
    }

    /** The shipment must not exceed the packaging's maximum weight (equal is allowed). */
    protected boolean withinMaxWeight(Shipment shipment, Packaging packaging) {
        return shipment.weight().compareTo(packaging.maxWeight()) <= 0;
    }

    protected boolean fitsDimensions(Shipment shipment, Packaging packaging) {
        return switch (packaging.type()) {
            case BOX -> boxFits(shipment, packaging);
            case BAG -> bagFits(shipment, packaging);
        };
    }

    /** Both triples sorted descending and compared pairwise: packaging >= shipment in every position. */
    protected boolean boxFits(Shipment shipment, Packaging box) {
        List<BigDecimal> ship = shipment.sortedDescending();
        List<BigDecimal> pack = sortedDescending(box.dimensions());
        for (int i = 0; i < 3; i++) {
            if (pack.get(i).compareTo(ship.get(i)) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Shipment sorted descending: Max, Mid, Min. Packaging sorted descending: length, width, thickness.
     * Fits when Max + Min <= length AND Mid + Min <= width (packaging thickness is not part of the check).
     */
    protected boolean bagFits(Shipment shipment, Packaging bag) {
        List<BigDecimal> ship = shipment.sortedDescending();
        List<BigDecimal> pack = sortedDescending(bag.dimensions());
        BigDecimal max = ship.get(0);
        BigDecimal mid = ship.get(1);
        BigDecimal min = ship.get(2);
        return max.add(min).compareTo(pack.get(0)) <= 0
                && mid.add(min).compareTo(pack.get(1)) <= 0;
    }

    protected static List<BigDecimal> sortedDescending(List<BigDecimal> dimensions) {
        return dimensions.stream().sorted(Comparator.reverseOrder()).toList();
    }
}
