package qa.promo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

/** Sends an {@link ApiCase} with REST Assured and lists every way the response differs from the expectation. */
public final class ApiCaseRunner {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    private static final List<String> AMOUNT_FIELDS = List.of("subtotal", "discount", "shipping", "total");

    private ApiCaseRunner() {
    }

    public static Response send(String baseUri, String body) {
        return given()
                .baseUri(baseUri)
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(PromoApplication.PATH);
    }

    /** @return human-readable differences; empty when the case passes */
    public static List<String> mismatches(String baseUri, ApiCase apiCase) {
        List<String> problems = new ArrayList<>();
        Response response;
        try {
            response = send(baseUri, apiCase.body());
        } catch (RuntimeException e) {
            return List.of("no usable response: " + e);
        }
        ApiCase.Expected expected = apiCase.expected();

        if (response.statusCode() != expected.status()) {
            problems.add("HTTP status: expected " + expected.status() + " but was " + response.statusCode());
        }
        if (response.contentType() == null || !response.contentType().startsWith("application/json")) {
            problems.add("Content-Type: expected application/json but was " + response.contentType());
        }

        JsonNode result;
        try {
            result = MAPPER.readTree(response.asString()).path("result");
        } catch (Exception e) {
            problems.add("body is not valid JSON: " + e.getMessage());
            return problems;
        }

        String message = result.path("message").asText(null);
        if (!expected.message().equals(message)) {
            problems.add("message: expected '" + expected.message() + "' but was '" + message + "'");
        }

        if (expected.isSuccess()) {
            compare(problems, result, "subtotal", expected.subtotal());
            compare(problems, result, "discount", expected.discount());
            compare(problems, result, "shipping", expected.shipping());
            compare(problems, result, "total", expected.total());
        } else {
            for (String field : AMOUNT_FIELDS) {
                if (result.has(field)) {
                    problems.add("error response must not contain '" + field + "'");
                }
            }
        }
        return problems;
    }

    private static void compare(List<String> problems, JsonNode result, String field, BigDecimal expected) {
        JsonNode node = result.get(field);
        if (node == null || !node.isNumber()) {
            problems.add(field + ": expected " + expected + " but the field is missing or not a number");
        } else if (node.decimalValue().compareTo(expected) != 0) {
            problems.add(field + ": expected " + expected + " but was " + node.decimalValue());
        }
    }
}
