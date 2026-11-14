package qa.promo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param type  PERCENT: {@code value} is a percentage (10 = 10 %); FIXED: {@code value} is an amount in dollars
 * @param usageLimit maximum number of redemptions; {@code used} redemptions have happened already
 */
public record PromoCode(String code, Type type, BigDecimal value, Instant expiresAt, int usageLimit, int used) {

    public enum Type {PERCENT, FIXED}
}
