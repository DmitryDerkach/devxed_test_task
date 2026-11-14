package qa.packaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** White-box checks of the shipment-shape rule (no packaging involved). */
class ShipmentTest {

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    private static Item item(String weight, String length, String width, String thickness) {
        return new Item(d(weight), d(length), d(width), d(thickness));
    }

    @Test
    @DisplayName("single item: shipment equals the item")
    void singleItem() {
        Shipment shipment = Shipment.of(List.of(item("1.5", "10", "5", "2")));

        assertThat(shipment).isEqualTo(new Shipment(d("1.5"), d("10"), d("5"), d("2")));
    }

    @Test
    @DisplayName("several items: max length, max width, summed thickness and weight")
    void severalItems() {
        Shipment shipment = Shipment.of(List.of(
                item("1", "5", "9", "1"),
                item("2", "12", "4", "3"),
                item("0.5", "8", "8", "0.5")));

        assertThat(shipment.length()).isEqualByComparingTo("12");
        assertThat(shipment.width()).isEqualByComparingTo("9");
        assertThat(shipment.thickness()).isEqualByComparingTo("4.5");
        assertThat(shipment.weight()).isEqualByComparingTo("3.5");
    }

    @Test
    @DisplayName("decimal sums are exact (50 x 0.1 == 5.0)")
    void exactDecimalSums() {
        List<Item> items = java.util.Collections.nCopies(50, item("0.1", "10", "10", "0.1"));

        Shipment shipment = Shipment.of(items);

        assertThat(shipment.weight()).isEqualByComparingTo("5.0");
        assertThat(shipment.thickness()).isEqualByComparingTo("5.0");
    }

    @Test
    @DisplayName("sortedDescending returns Max, Mid, Min regardless of input order")
    void sortedDescending() {
        Shipment shipment = new Shipment(d("1"), d("2"), d("10"), d("6"));

        assertThat(shipment.sortedDescending()).containsExactly(d("10"), d("6"), d("2"));
    }

    @Test
    @DisplayName("empty item list is rejected")
    void emptyList() {
        assertThatThrownBy(() -> Shipment.of(List.of())).isInstanceOf(IllegalArgumentException.class);
    }
}
