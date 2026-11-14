package qa.promo;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;

import static io.restassured.http.ContentType.JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * POST /api/apply-promo-code - black-box tests against the service started on a random port.
 * Data-driven groups mirror the checklist sections (IDs = test-case IDs); the rest are hand-written contract checks.
 */
class ApplyPromoCodeApiTest {

    private static PromoApplication app;

    @BeforeAll
    static void startService() throws IOException {
        app = PromoApplication.start(new PromoService(new InMemoryPromoCodeRepository()), 0);
    }

    @AfterAll
    static void stopService() {
        app.close();
    }

    static Stream<ApiCase> happy() {
        return ApiCases.group("HAPPY").stream();
    }

    static Stream<ApiCase> gift() {
        return ApiCases.group("GIFT").stream();
    }

    static Stream<ApiCase> shipping() {
        return ApiCases.group("SHIP").stream();
    }

    static Stream<ApiCase> promoCode() {
        return ApiCases.group("CODE").stream();
    }

    static Stream<ApiCase> items() {
        return ApiCases.group("ITEMS").stream();
    }

    @DisplayName("Discount and totals")
    @ParameterizedTest(name = "{0}")
    @MethodSource("happy")
    void discountAndTotals(ApiCase apiCase) {
        verify(apiCase);
    }

    @DisplayName("gift_card handling")
    @ParameterizedTest(name = "{0}")
    @MethodSource("gift")
    void giftCards(ApiCase apiCase) {
        verify(apiCase);
    }

    @DisplayName("Shipping")
    @ParameterizedTest(name = "{0}")
    @MethodSource("shipping")
    void shipping(ApiCase apiCase) {
        verify(apiCase);
    }

    @DisplayName("Promo code validation")
    @ParameterizedTest(name = "{0}")
    @MethodSource("promoCode")
    void promoCodeValidation(ApiCase apiCase) {
        verify(apiCase);
    }

    @DisplayName("Cart items validation")
    @ParameterizedTest(name = "{0}")
    @MethodSource("items")
    void cartItemsValidation(ApiCase apiCase) {
        verify(apiCase);
    }

    private void verify(ApiCase apiCase) {
        assertThat(ApiCaseRunner.mismatches(app.baseUri(), apiCase))
                .as("%s [%s]", apiCase.id(), apiCase.technique())
                .isEmpty();
    }

    // ------------------------------------------------------------------ hand-written contract checks

    private static final String PHYSICAL_50 = """
            {"promo_code": "SAVE10", "items": [
              {"product_id": "SKU-1", "product_type": "physical", "count": 1, "price": 50.00}]}""";

    @Test
    @DisplayName("PC-30.1 success response has exactly the documented fields")
    void responseHasDocumentedStructure() {
        ApiCaseRunner.send(app.baseUri(), PHYSICAL_50)
                .then()
                .statusCode(200)
                .contentType(JSON)
                .body("result.keySet()", containsInAnyOrder("message", "subtotal", "discount", "shipping", "total"))
                .body("result.message", instanceOf(String.class))
                .body("result.subtotal", instanceOf(Number.class))
                .body("result.discount", instanceOf(Number.class))
                .body("result.shipping", instanceOf(Number.class))
                .body("result.total", instanceOf(Number.class));
    }

    @Test
    @DisplayName("PC-31.1 the same request twice returns the same response (applying does not consume the code)")
    void sameRequestTwiceReturnsSameResponse() {
        String first = ApiCaseRunner.send(app.baseUri(), PHYSICAL_50).asString();
        String second = ApiCaseRunner.send(app.baseUri(), PHYSICAL_50).asString();

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("PC-31.2 the order of the cart lines does not change the result")
    void itemOrderDoesNotChangeTheResult() {
        String line1 = "{\"product_id\": \"A\", \"product_type\": \"physical\", \"count\": 1, \"price\": 20.00}";
        String line2 = "{\"product_id\": \"B\", \"product_type\": \"gift_card\", \"count\": 1, \"price\": 30.00}";
        String line3 = "{\"product_id\": \"C\", \"product_type\": \"digital\", \"count\": 2, \"price\": 5.50}";

        String forward = "{\"promo_code\": \"SAVE10\", \"items\": [" + line1 + "," + line2 + "," + line3 + "]}";
        String reversed = "{\"promo_code\": \"SAVE10\", \"items\": [" + line3 + "," + line2 + "," + line1 + "]}";

        String forwardResponse = ApiCaseRunner.send(app.baseUri(), forward).asString();
        String reversedResponse = ApiCaseRunner.send(app.baseUri(), reversed).asString();

        assertThat(reversedResponse).isEqualTo(forwardResponse);
        ApiCaseRunner.send(app.baseUri(), forward).then().statusCode(200)
                .body("result.subtotal", equalTo(61.0f))
                .body("result.discount", equalTo(3.1f));
    }
}
