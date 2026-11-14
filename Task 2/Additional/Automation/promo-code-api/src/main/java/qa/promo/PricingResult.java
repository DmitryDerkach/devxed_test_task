package qa.promo;

import java.math.BigDecimal;

public record PricingResult(String message, BigDecimal subtotal, BigDecimal discount, BigDecimal shipping,
                            BigDecimal total) {
}
