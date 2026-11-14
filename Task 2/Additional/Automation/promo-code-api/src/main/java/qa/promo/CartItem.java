package qa.promo;

import java.math.BigDecimal;

/** A validated cart line. */
public record CartItem(String productId, ProductType type, int count, BigDecimal price) {
}
