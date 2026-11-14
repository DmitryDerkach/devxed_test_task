package qa.promo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Fixture promo codes documented in the "Assumptions" sheet. Keys are upper-case. */
public final class InMemoryPromoCodeRepository implements PromoCodeRepository {

    private static final Instant FAR_FUTURE = Instant.parse("2099-12-31T23:59:59Z");
    private static final Instant LONG_AGO = Instant.parse("2020-01-01T00:00:00Z");

    private final Map<String, PromoCode> codes = new HashMap<>();

    public InMemoryPromoCodeRepository() {
        add(new PromoCode("SAVE10", PromoCode.Type.PERCENT, new BigDecimal("10"), FAR_FUTURE, 1_000_000, 0));
        add(new PromoCode("FLAT5", PromoCode.Type.FIXED, new BigDecimal("5.00"), FAR_FUTURE, 1_000_000, 0));
        add(new PromoCode("EXPIRED20", PromoCode.Type.PERCENT, new BigDecimal("20"), LONG_AGO, 1_000_000, 0));
        add(new PromoCode("USEDUP", PromoCode.Type.PERCENT, new BigDecimal("15"), FAR_FUTURE, 100, 100));
    }

    public InMemoryPromoCodeRepository add(PromoCode code) {
        codes.put(code.code(), code);
        return this;
    }

    @Override
    public Optional<PromoCode> find(String normalizedCode) {
        return Optional.ofNullable(codes.get(normalizedCode));
    }
}
