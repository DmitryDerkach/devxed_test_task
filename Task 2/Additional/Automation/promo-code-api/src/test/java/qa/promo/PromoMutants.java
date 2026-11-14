package qa.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

/**
 * Deliberately broken variants of {@link PromoService}. The API test-suite must fail on every one of them;
 * {@link MutationSanityTest} proves it.
 */
final class PromoMutants {

    record Mutant(String name, PromoService service) {
        @Override
        public String toString() {
            return name;
        }
    }

    private PromoMutants() {
    }

    private static InMemoryPromoCodeRepository repo() {
        return new InMemoryPromoCodeRepository();
    }

    static List<Mutant> all() {
        return List.of(
                new Mutant("GIFT_CARDS_ARE_DISCOUNTED", new PromoService(repo()) {
                    @Override
                    protected BigDecimal eligibleAmount(List<CartItem> items) {
                        return items.stream().map(this::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.UNNECESSARY);
                    }
                }),
                new Mutant("GIFT_CARD_ONLY_CART_ACCEPTED_WITH_ZERO_DISCOUNT", new PromoService(repo()) {
                    @Override
                    protected boolean acceptsCartWithoutEligibleItems() {
                        return true;
                    }
                }),
                new Mutant("FREE_SHIPPING_CHECKED_AFTER_DISCOUNT", new PromoService(repo()) {
                    @Override
                    protected boolean qualifiesForFreeShipping(BigDecimal subtotal, BigDecimal discount) {
                        return subtotal.subtract(discount).compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
                    }
                }),
                new Mutant("FREE_SHIPPING_STRICT (> 35.00 instead of >=)", new PromoService(repo()) {
                    @Override
                    protected boolean qualifiesForFreeShipping(BigDecimal subtotal, BigDecimal discount) {
                        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) > 0;
                    }
                }),
                new Mutant("DIGITAL_ONLY_CART_STILL_CHARGED_SHIPPING", new PromoService(repo()) {
                    @Override
                    protected boolean isDigitalOnly(List<CartItem> items) {
                        return false;
                    }
                }),
                new Mutant("GIFT_CARD_TREATED_AS_DIGITAL_FOR_SHIPPING", new PromoService(repo()) {
                    @Override
                    protected boolean isDigitalOnly(List<CartItem> items) {
                        return items.stream().allMatch(i -> i.type() != ProductType.PHYSICAL);
                    }
                }),
                new Mutant("DISCOUNT_ROUNDED_DOWN (instead of HALF_UP)", new PromoService(repo()) {
                    @Override
                    protected RoundingMode rounding() {
                        return RoundingMode.DOWN;
                    }
                }),
                new Mutant("DISCOUNT_NOT_CAPPED (total can drop below shipping)", new PromoService(repo()) {
                    @Override
                    protected BigDecimal discountCap(BigDecimal eligible, BigDecimal subtotal) {
                        return new BigDecimal("1000000000");
                    }
                }),
                new Mutant("DISCOUNT_CAP_USES_WHOLE_SUBTOTAL (incl. gift cards)", new PromoService(repo()) {
                    @Override
                    protected BigDecimal discountCap(BigDecimal eligible, BigDecimal subtotal) {
                        return subtotal;
                    }
                }),
                new Mutant("PERCENT_DISCOUNT_TAKEN_FROM_SUBTOTAL_PLUS_SHIPPING", new PromoService(repo()) {
                    @Override
                    protected BigDecimal discountBase(BigDecimal eligible, BigDecimal subtotal) {
                        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) < 0 ? eligible.add(SHIPPING_FEE) : eligible;
                    }
                }),
                new Mutant("PROMO_CODE_CASE_SENSITIVE", new PromoService(repo()) {
                    @Override
                    protected String normalize(String raw) {
                        return raw == null ? "" : raw.trim();
                    }
                }),
                new Mutant("PROMO_CODE_NOT_TRIMMED", new PromoService(repo()) {
                    @Override
                    protected String normalize(String raw) {
                        return raw == null ? "" : raw.toUpperCase(Locale.ROOT);
                    }
                }),
                new Mutant("EXPIRED_CODE_ACCEPTED", new PromoService(repo()) {
                    @Override
                    protected boolean isExpired(PromoCode promo) {
                        return false;
                    }
                }),
                new Mutant("USAGE_LIMIT_IGNORED", new PromoService(repo()) {
                    @Override
                    protected boolean isUsageLimitReached(PromoCode promo) {
                        return false;
                    }
                }),
                new Mutant("ITEM_COUNT_UPPER_LIMIT_IS_1000", new PromoService(repo()) {
                    @Override
                    protected int maxCount() {
                        return 1000;
                    }
                }),
                new Mutant("PRICE_DECIMAL_PLACES_NOT_VALIDATED", new PromoService(repo()) {
                    @Override
                    protected boolean hasTooManyDecimals(BigDecimal price) {
                        return false;
                    }
                }),
                new Mutant("COUNT_IGNORED_IN_LINE_TOTAL", new PromoService(repo()) {
                    @Override
                    protected BigDecimal lineTotal(CartItem item) {
                        return item.price();
                    }
                })
        );
    }
}
