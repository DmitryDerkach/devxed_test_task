package qa.packaging;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Deliberately broken variants of the algorithm. A good test-suite must fail on every one of them
 * ("kill the mutant"); {@link MutationSanityTest} checks exactly that.
 */
final class Mutants {

    record Mutant(String name, PackagingSelector selector) {
        @Override
        public String toString() {
            return name;
        }
    }

    private Mutants() {
    }

    private static Mutant withRules(String name, FitRules rules) {
        return new Mutant(name, new PackagingSelector(ShipmentShaper.STANDARD, rules, PackagingSelector.CHEAPEST_FIRST));
    }

    private static Mutant withShaper(String name, ShipmentShaper shaper) {
        return new Mutant(name, new PackagingSelector(shaper, FitRules.STANDARD, PackagingSelector.CHEAPEST_FIRST));
    }

    private static Mutant withPreference(String name, Comparator<Packaging> preference) {
        return new Mutant(name, new PackagingSelector(ShipmentShaper.STANDARD, FitRules.STANDARD, preference));
    }

    static List<Mutant> all() {
        return List.of(
                // ---- box rule ----
                withRules("BOX_STRICT_COMPARISON (> instead of >=)", new StandardFitRules() {
                    @Override
                    protected boolean boxFits(Shipment s, Packaging box) {
                        List<BigDecimal> ship = s.sortedDescending();
                        List<BigDecimal> pack = sortedDescending(box.dimensions());
                        for (int i = 0; i < 3; i++) {
                            if (pack.get(i).compareTo(ship.get(i)) <= 0) {
                                return false;
                            }
                        }
                        return true;
                    }
                }),
                withRules("BOX_NO_SORTING (dimensions compared as written)", new StandardFitRules() {
                    @Override
                    protected boolean boxFits(Shipment s, Packaging box) {
                        List<BigDecimal> ship = List.of(s.length(), s.width(), s.thickness());
                        for (int i = 0; i < 3; i++) {
                            if (box.dimensions().get(i).compareTo(ship.get(i)) < 0) {
                                return false;
                            }
                        }
                        return true;
                    }
                }),

                // ---- bag rule ----
                withRules("BAG_STRICT_COMPARISON (< instead of <=)", new StandardFitRules() {
                    @Override
                    protected boolean bagFits(Shipment s, Packaging bag) {
                        List<BigDecimal> ship = s.sortedDescending();
                        List<BigDecimal> pack = sortedDescending(bag.dimensions());
                        return ship.get(0).add(ship.get(2)).compareTo(pack.get(0)) < 0
                                && ship.get(1).add(ship.get(2)).compareTo(pack.get(1)) < 0;
                    }
                }),
                withRules("BAG_WRONG_FORMULA (Max+Mid instead of Max+Min)", new StandardFitRules() {
                    @Override
                    protected boolean bagFits(Shipment s, Packaging bag) {
                        List<BigDecimal> ship = s.sortedDescending();
                        List<BigDecimal> pack = sortedDescending(bag.dimensions());
                        return ship.get(0).add(ship.get(1)).compareTo(pack.get(0)) <= 0
                                && ship.get(1).add(ship.get(2)).compareTo(pack.get(1)) <= 0;
                    }
                }),
                withRules("BAG_PACKAGING_NOT_SORTED", new StandardFitRules() {
                    @Override
                    protected boolean bagFits(Shipment s, Packaging bag) {
                        List<BigDecimal> ship = s.sortedDescending();
                        List<BigDecimal> pack = bag.dimensions();
                        return ship.get(0).add(ship.get(2)).compareTo(pack.get(0)) <= 0
                                && ship.get(1).add(ship.get(2)).compareTo(pack.get(1)) <= 0;
                    }
                }),
                withRules("BAG_SHIPMENT_NOT_SORTED", new StandardFitRules() {
                    @Override
                    protected boolean bagFits(Shipment s, Packaging bag) {
                        List<BigDecimal> pack = sortedDescending(bag.dimensions());
                        return s.length().add(s.thickness()).compareTo(pack.get(0)) <= 0
                                && s.width().add(s.thickness()).compareTo(pack.get(1)) <= 0;
                    }
                }),
                withRules("BAG_ALSO_CHECKS_THICKNESS (extra rule)", new StandardFitRules() {
                    @Override
                    protected boolean bagFits(Shipment s, Packaging bag) {
                        return super.bagFits(s, bag)
                                && sortedDescending(bag.dimensions()).get(2).compareTo(s.sortedDescending().get(2)) >= 0;
                    }
                }),

                // ---- filters ----
                withRules("AVAILABILITY_IGNORED", new StandardFitRules() {
                    @Override
                    public boolean isSuitable(Shipment s, Packaging p) {
                        return withinMaxWeight(s, p) && fitsDimensions(s, p);
                    }
                }),
                withRules("MAX_WEIGHT_IGNORED", new StandardFitRules() {
                    @Override
                    protected boolean withinMaxWeight(Shipment s, Packaging p) {
                        return true;
                    }
                }),
                withRules("MAX_WEIGHT_EXCLUSIVE (< instead of <=)", new StandardFitRules() {
                    @Override
                    protected boolean withinMaxWeight(Shipment s, Packaging p) {
                        return s.weight().compareTo(p.maxWeight()) < 0;
                    }
                }),

                // ---- shipment shape ----
                withShaper("THICKNESS_IS_MAX_NOT_SUM", items -> new Shipment(
                        sum(items, Item::weight),
                        max(items, Item::length),
                        max(items, Item::width),
                        max(items, Item::thickness))),
                withShaper("WEIGHT_IS_MAX_NOT_SUM", items -> new Shipment(
                        max(items, Item::weight),
                        max(items, Item::length),
                        max(items, Item::width),
                        sum(items, Item::thickness))),
                withShaper("LENGTH_AND_WIDTH_FROM_FIRST_ITEM", items -> new Shipment(
                        sum(items, Item::weight),
                        items.get(0).length(),
                        items.get(0).width(),
                        sum(items, Item::thickness))),
                withShaper("DOUBLE_ARITHMETIC_INSTEAD_OF_DECIMAL", items -> {
                    double weight = 0;
                    double thickness = 0;
                    for (Item item : items) {
                        weight += item.weight().doubleValue();
                        thickness += item.thickness().doubleValue();
                    }
                    return new Shipment(BigDecimal.valueOf(weight),
                            max(items, Item::length),
                            max(items, Item::width),
                            BigDecimal.valueOf(thickness));
                }),

                // ---- choice ----
                withPreference("MOST_EXPENSIVE_INSTEAD_OF_CHEAPEST",
                        Comparator.comparing(Packaging::cost).reversed()),
                withPreference("NO_TIE_BREAK (first in input order wins)",
                        Comparator.comparing(Packaging::cost))
        );
    }

    private static BigDecimal sum(List<Item> items, java.util.function.Function<Item, BigDecimal> field) {
        return items.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal max(List<Item> items, java.util.function.Function<Item, BigDecimal> field) {
        return items.stream().map(field).reduce(BigDecimal.ZERO, BigDecimal::max);
    }
}
