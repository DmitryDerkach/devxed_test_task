package qa.promo;

import java.util.Optional;

public interface PromoCodeRepository {

    /** @param normalizedCode the code after the service has trimmed / upper-cased it */
    Optional<PromoCode> find(String normalizedCode);
}
