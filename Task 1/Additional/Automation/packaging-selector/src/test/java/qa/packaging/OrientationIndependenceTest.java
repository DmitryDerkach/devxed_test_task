package qa.packaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The order in which dimensions are written down must never change the verdict (equivalence of permutations). */
class OrientationIndependenceTest {

    private enum Permutation {
        LWT(0, 1, 2), LTW(0, 2, 1), WLT(1, 0, 2), WTL(1, 2, 0), TLW(2, 0, 1), TWL(2, 1, 0);

        private final int[] order;

        Permutation(int... order) {
            this.order = order;
        }

        <T> List<T> apply(List<T> source) {
            return List.of(source.get(order[0]), source.get(order[1]), source.get(order[2]));
        }
    }

    private static List<BigDecimal> dims(String... values) {
        return java.util.Arrays.stream(values).map(BigDecimal::new).toList();
    }

    private static Shipment shipmentOf(List<BigDecimal> d) {
        return Shipment.of(List.of(new Item(BigDecimal.ONE, d.get(0), d.get(1), d.get(2))));
    }

    private static Packaging packaging(PackagingType type, List<BigDecimal> dimensions) {
        return new Packaging("P", BigDecimal.ONE, true, type, BigDecimal.TEN, dimensions);
    }

    @DisplayName("box verdict is the same for every ordering of item and box dimensions")
    @ParameterizedTest(name = "item as {0}")
    @EnumSource(Permutation.class)
    void boxVerdictIgnoresOrder(Permutation itemOrder) {
        for (Permutation boxOrder : Permutation.values()) {
            Shipment shipment = shipmentOf(itemOrder.apply(dims("10", "6", "4")));

            boolean exact = FitRules.STANDARD.isSuitable(shipment,
                    packaging(PackagingType.BOX, boxOrder.apply(dims("10", "6", "4"))));
            boolean tooShort = FitRules.STANDARD.isSuitable(shipment,
                    packaging(PackagingType.BOX, boxOrder.apply(dims("10", "6", "3.99"))));

            assertThat(exact).as("exact box, box order %s", boxOrder).isTrue();
            assertThat(tooShort).as("short box, box order %s", boxOrder).isFalse();
        }
    }

    @DisplayName("bag verdict is the same for every ordering of item and bag dimensions")
    @ParameterizedTest(name = "item as {0}")
    @EnumSource(Permutation.class)
    void bagVerdictIgnoresOrder(Permutation itemOrder) {
        for (Permutation bagOrder : Permutation.values()) {
            Shipment shipment = shipmentOf(itemOrder.apply(dims("10", "6", "2")));

            boolean exact = FitRules.STANDARD.isSuitable(shipment,
                    packaging(PackagingType.BAG, bagOrder.apply(dims("12", "8", "1"))));
            boolean tooNarrow = FitRules.STANDARD.isSuitable(shipment,
                    packaging(PackagingType.BAG, bagOrder.apply(dims("12", "7.99", "1"))));

            assertThat(exact).as("exact bag, bag order %s", bagOrder).isTrue();
            assertThat(tooNarrow).as("narrow bag, bag order %s", bagOrder).isFalse();
        }
    }
}
