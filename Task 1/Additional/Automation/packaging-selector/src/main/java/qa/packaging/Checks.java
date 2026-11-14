package qa.packaging;

import java.math.BigDecimal;

/** Tiny argument-validation helpers shared by the domain records. */
final class Checks {

    private Checks() {
    }

    static BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(field + " must be greater than 0 but was " + value);
        }
        return value;
    }

    static BigDecimal nonNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative but was " + value);
        }
        return value;
    }
}
