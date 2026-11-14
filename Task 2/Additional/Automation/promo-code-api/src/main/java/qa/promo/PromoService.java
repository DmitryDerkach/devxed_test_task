package qa.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Business logic of POST /api/apply-promo-code (see the "Assumptions" sheet for every rule that is not in the spec).
 *
 * <p>Validation order: cart first, then promo code (required, known, not expired, usage limit, applicable).
 * Every individual rule is a small protected method so tests can derive deliberately broken variants (mutants).
 */
public class PromoService {

    public static final String SUCCESS_MESSAGE = "Promo code applied successfully";

    protected static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("35.00");
    protected static final BigDecimal SHIPPING_FEE = new BigDecimal("4.99");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final BigDecimal MAX_PRICE = new BigDecimal("9999.99");
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final PromoCodeRepository repository;
    private final Clock clock;

    public PromoService(PromoCodeRepository repository) {
        this(repository, Clock.systemUTC());
    }

    public PromoService(PromoCodeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public PricingResult apply(String rawCode, List<RawItem> rawItems) {
        List<CartItem> items = validateCart(rawItems);

        String code = normalize(rawCode);
        if (code.isEmpty()) {
            throw new ValidationException("Promo code is required");
        }
        PromoCode promo = repository.find(code)
                .orElseThrow(() -> new ValidationException("Promo code is invalid"));
        if (isExpired(promo)) {
            throw new ValidationException("Promo code has expired");
        }
        if (isUsageLimitReached(promo)) {
            throw new ValidationException("Promo code usage limit has been reached");
        }

        BigDecimal subtotal = items.stream().map(this::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
        BigDecimal eligible = eligibleAmount(items);
        if (eligible.signum() == 0 && !acceptsCartWithoutEligibleItems()) {
            throw new ValidationException("Promo code is not applicable to the items in the cart");
        }

        BigDecimal discount = calculateDiscount(promo, eligible, subtotal);
        BigDecimal shipping = shipping(items, subtotal, discount);
        BigDecimal total = subtotal.subtract(discount).add(shipping);
        return new PricingResult(SUCCESS_MESSAGE, subtotal, discount, shipping, total);
    }

    // ------------------------------------------------------------------ cart validation

    private List<CartItem> validateCart(List<RawItem> rawItems) {
        if (rawItems == null || rawItems.isEmpty()) {
            throw new ValidationException("Cart must contain at least one item");
        }
        List<CartItem> items = new ArrayList<>();
        for (RawItem raw : rawItems) {
            if (raw.productId() == null || raw.productId().isBlank()) {
                throw new ValidationException("Product id is required");
            }
            ProductType type = ProductType.fromWire(raw.productType())
                    .orElseThrow(() -> new ValidationException("Product type must be one of: physical, digital, gift_card"));
            if (raw.count() == null || raw.count() < 1 || raw.count() > maxCount()) {
                throw new ValidationException("Item count must be an integer between 1 and 999");
            }
            BigDecimal price = raw.price();
            if (price == null || price.compareTo(MIN_PRICE) < 0 || price.compareTo(MAX_PRICE) > 0
                    || hasTooManyDecimals(price)) {
                throw new ValidationException("Item price must be between 0.01 and 9999.99 with at most 2 decimal places");
            }
            items.add(new CartItem(raw.productId(), type, raw.count().intValue(), price));
        }
        return items;
    }

    protected int maxCount() {
        return 999;
    }

    protected boolean hasTooManyDecimals(BigDecimal price) {
        return price.stripTrailingZeros().scale() > 2;
    }

    // ------------------------------------------------------------------ promo code rules

    /** Codes are trimmed and case-insensitive. */
    protected String normalize(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    protected boolean isExpired(PromoCode promo) {
        return !clock.instant().isBefore(promo.expiresAt());
    }

    protected boolean isUsageLimitReached(PromoCode promo) {
        return promo.used() >= promo.usageLimit();
    }

    /** gift_card items are excluded from the discount. */
    protected BigDecimal eligibleAmount(List<CartItem> items) {
        return items.stream()
                .filter(item -> item.type() != ProductType.GIFT_CARD)
                .map(this::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

    protected boolean acceptsCartWithoutEligibleItems() {
        return false;
    }

    protected BigDecimal lineTotal(CartItem item) {
        return item.price().multiply(BigDecimal.valueOf(item.count()));
    }

    // ------------------------------------------------------------------ discount

    protected BigDecimal calculateDiscount(PromoCode promo, BigDecimal eligible, BigDecimal subtotal) {
        BigDecimal raw = switch (promo.type()) {
            case PERCENT -> discountBase(eligible, subtotal).multiply(promo.value()).movePointLeft(2);
            case FIXED -> promo.value();
        };
        return raw.setScale(2, rounding()).min(discountCap(eligible, subtotal));
    }

    protected RoundingMode rounding() {
        return RoundingMode.HALF_UP;
    }

    protected BigDecimal discountBase(BigDecimal eligible, BigDecimal subtotal) {
        return eligible;
    }

    /** The discount can never exceed what is eligible, so the total never goes below shipping. */
    protected BigDecimal discountCap(BigDecimal eligible, BigDecimal subtotal) {
        return eligible;
    }

    // ------------------------------------------------------------------ shipping

    protected BigDecimal shipping(List<CartItem> items, BigDecimal subtotal, BigDecimal discount) {
        if (isDigitalOnly(items)) {
            return ZERO;
        }
        return qualifiesForFreeShipping(subtotal, discount) ? ZERO : SHIPPING_FEE;
    }

    /** Evaluated on the subtotal BEFORE the discount; 35.00 itself already qualifies. */
    protected boolean qualifiesForFreeShipping(BigDecimal subtotal, BigDecimal discount) {
        return subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0;
    }

    /** Strict reading of the spec: only carts consisting of digital items alone ship for free. */
    protected boolean isDigitalOnly(List<CartItem> items) {
        return items.stream().allMatch(item -> item.type() == ProductType.DIGITAL);
    }
}
