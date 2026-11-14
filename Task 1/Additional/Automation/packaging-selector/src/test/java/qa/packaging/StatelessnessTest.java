package qa.packaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** PK-31.1: no hidden state - repeated calls agree and the caller's lists are left untouched. */
class StatelessnessTest {

    private static Packaging box(String name, String cost, boolean available) {
        return new Packaging(name, new BigDecimal(cost), available, PackagingType.BOX, BigDecimal.TEN,
                List.of(new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20")));
    }

    @Test
    @DisplayName("PK-31.1 same input twice gives the same answer; inputs are not mutated")
    void repeatedCallsAreIdenticalAndInputsUntouched() {
        PackagingSelector selector = new PackagingSelector();
        List<Item> items = new ArrayList<>(List.of(
                new Item(BigDecimal.ONE, new BigDecimal("10"), new BigDecimal("6"), new BigDecimal("2"))));
        List<Packaging> catalog = new ArrayList<>(List.of(box("Expensive", "3", true), box("Cheap", "1", true)));
        List<Item> itemsBefore = List.copyOf(items);
        List<Packaging> catalogBefore = List.copyOf(catalog);

        Optional<Packaging> first = selector.select(items, catalog);
        Optional<Packaging> second = selector.select(items, catalog);

        assertThat(first).isEqualTo(second).get().extracting(Packaging::name).isEqualTo("Cheap");
        assertThat(items).isEqualTo(itemsBefore);
        assertThat(catalog).isEqualTo(catalogBefore);
    }

    @Test
    @DisplayName("PK-31.1 availability toggled between calls is reflected immediately")
    void availabilityToggleIsPickedUpOnNextCall() {
        PackagingSelector selector = new PackagingSelector();
        List<Item> items = List.of(
                new Item(BigDecimal.ONE, new BigDecimal("10"), new BigDecimal("6"), new BigDecimal("2")));

        Optional<Packaging> whileAvailable =
                selector.select(items, List.of(box("Cheap", "1", true), box("Next", "2", true)));
        Optional<Packaging> afterSellOut =
                selector.select(items, List.of(box("Cheap", "1", false), box("Next", "2", true)));
        Optional<Packaging> afterRestock =
                selector.select(items, List.of(box("Cheap", "1", true), box("Next", "2", true)));

        assertThat(whileAvailable).get().extracting(Packaging::name).isEqualTo("Cheap");
        assertThat(afterSellOut).get().extracting(Packaging::name).isEqualTo("Next");
        assertThat(afterRestock).get().extracting(Packaging::name).isEqualTo("Cheap");
    }
}
