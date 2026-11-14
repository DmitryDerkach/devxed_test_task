package qa.promo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Minimal HTTP wrapper (JDK built-in server, no framework) around {@link PromoService}.
 * Run it with {@code ./mvnw compile exec:java} (port 8080, override with PORT) for Postman / Newman.
 */
public final class PromoApplication implements AutoCloseable {

    public static final String PATH = "/api/apply-promo-code";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
            .configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);

    private final HttpServer server;
    private final ExecutorService executor;

    private PromoApplication(HttpServer server, ExecutorService executor) {
        this.server = server;
        this.executor = executor;
    }

    /** @param port 0 picks a free port (used by the tests) */
    public static PromoApplication start(PromoService service, int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        ExecutorService executor = Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "promo-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext(PATH, exchange -> handle(service, exchange));
        server.start();
        return new PromoApplication(server, executor);
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public String baseUri() {
        return "http://localhost:" + port();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", args.length > 0 ? args[0] : "8080"));
        PromoApplication app = start(new PromoService(new InMemoryPromoCodeRepository()), port);
        System.out.println("promo-code-api listening on " + app.baseUri() + PATH + "  (Ctrl+C to stop)");
        // The HTTP threads are daemons (so tests can shut down cleanly) - keep the JVM alive when run as a service.
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ request handling

    private static void handle(PromoService service, HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"POST".equals(exchange.getRequestMethod())) {
                respond(exchange, 405, errorBody("Method not allowed"));
                return;
            }
            JsonNode root;
            try {
                root = MAPPER.readTree(exchange.getRequestBody());
            } catch (JsonProcessingException e) {
                respond(exchange, 400, errorBody("Malformed JSON request"));
                return;
            }
            if (root == null || !root.isObject()) {
                respond(exchange, 400, errorBody("Malformed JSON request"));
                return;
            }
            try {
                PricingResult result = service.apply(text(root.get("promo_code")), items(root.get("items")));
                respond(exchange, 200, successBody(result));
            } catch (ValidationException e) {
                respond(exchange, 400, errorBody(e.getMessage()));
            } catch (RuntimeException e) {
                respond(exchange, 500, errorBody("Internal error"));
            }
        }
    }

    private static List<RawItem> items(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<RawItem> items = new ArrayList<>();
        for (JsonNode line : node) {
            Long count = line.hasNonNull("count") && line.get("count").isNumber() ? line.get("count").asLong() : null;
            items.add(new RawItem(
                    text(line.get("product_id")),
                    text(line.get("product_type")),
                    count,
                    line.hasNonNull("price") && line.get("price").isNumber() ? line.get("price").decimalValue() : null));
        }
        return items;
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Map<String, Object> successBody(PricingResult r) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", r.message());
        result.put("subtotal", r.subtotal());
        result.put("discount", r.discount());
        result.put("shipping", r.shipping());
        result.put("total", r.total());
        return Map.of("result", result);
    }

    private static Map<String, Object> errorBody(String message) {
        return Map.of("result", Map.of("message", message));
    }

    private static void respond(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = MAPPER.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
