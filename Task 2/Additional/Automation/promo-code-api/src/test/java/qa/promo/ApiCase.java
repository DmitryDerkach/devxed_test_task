package qa.promo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * One API test case, loaded from {@code cases.json}. The same file generates the spreadsheet and the Postman
 * collection, so documented, Postman and Java expectations are identical by construction.
 *
 * @param body raw request body, sent exactly as written (so invalid / odd literals such as 10.001 survive)
 */
public record ApiCase(String id,
                      String group,
                      List<String> ref,
                      String technique,
                      String priority,
                      Map<String, String> title,
                      String body,
                      Expected expected) {

    /** Amounts are null for error responses (they must not be present at all). */
    public record Expected(int status, String message, BigDecimal subtotal, BigDecimal discount,
                           BigDecimal shipping, BigDecimal total) {
        public boolean isSuccess() {
            return status == 200;
        }
    }

    @Override
    public String toString() {
        return id + " " + title.get("en");
    }
}
