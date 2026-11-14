package qa.promo;

import java.math.BigDecimal;

/** A cart line exactly as received (anything may be missing); {@link PromoService} validates it. */
public record RawItem(String productId, String productType, Long count, BigDecimal price) {
}
