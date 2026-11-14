package qa.promo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Expiry boundary with a frozen clock (service-level test; the HTTP fixtures only use clearly past / future dates).
 * A code is valid strictly before {@code expiresAt} and expired from that instant on.
 */
class PromoCodeExpiryTest {

    private static final Instant EXPIRES_AT = Instant.parse("2030-01-01T00:00:00Z");

    private static final List<RawItem> CART =
            List.of(new RawItem("SKU-1", "physical", 1L, new BigDecimal("50.00")));

    private static PromoService serviceAt(String now) {
        InMemoryPromoCodeRepository repository = new InMemoryPromoCodeRepository()
                .add(new PromoCode("SPRING", PromoCode.Type.PERCENT, BigDecimal.TEN, EXPIRES_AT, 10, 0));
        return new PromoService(repository, Clock.fixed(Instant.parse(now), ZoneOffset.UTC));
    }

    @DisplayName("PC-18.2 code is valid until the expiry instant")
    @ParameterizedTest(name = "now = {0}")
    @CsvSource({"2029-12-31T23:59:59Z", "2029-06-01T12:00:00Z"})
    void validBeforeExpiry(String now) {
        PricingResult result = serviceAt(now).apply("SPRING", CART);

        assertThat(result.discount()).isEqualByComparingTo("5.00");
    }

    @DisplayName("PC-18.2 code is expired at the expiry instant and after it")
    @ParameterizedTest(name = "now = {0}")
    @CsvSource({"2030-01-01T00:00:00Z", "2030-01-01T00:00:01Z", "2031-01-01T00:00:00Z"})
    void expiredFromTheExpiryInstant(String now) {
        assertThatThrownBy(() -> serviceAt(now).apply("SPRING", CART))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Promo code has expired");
    }
}
