package qa.promo;

import java.util.Arrays;
import java.util.Optional;

public enum ProductType {
    PHYSICAL("physical"),
    DIGITAL("digital"),
    GIFT_CARD("gift_card");

    private final String wire;

    ProductType(String wire) {
        this.wire = wire;
    }

    /** Case-sensitive on purpose: the API contract lists exactly physical, digital, gift_card. */
    public static Optional<ProductType> fromWire(String value) {
        return Arrays.stream(values()).filter(t -> t.wire.equals(value)).findFirst();
    }
}
