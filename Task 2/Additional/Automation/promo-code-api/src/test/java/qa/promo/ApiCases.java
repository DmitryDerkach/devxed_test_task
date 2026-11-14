package qa.promo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Loads {@code /cases.json} from the test classpath. */
public final class ApiCases {

    private static final List<ApiCase> ALL = load();

    private ApiCases() {
    }

    public static List<ApiCase> all() {
        return ALL;
    }

    public static List<ApiCase> group(String group) {
        return ALL.stream().filter(c -> c.group().equals(group)).toList();
    }

    private static List<ApiCase> load() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = ApiCases.class.getResourceAsStream("/cases.json")) {
            if (in == null) {
                throw new IllegalStateException("cases.json not found on the test classpath");
            }
            return mapper.readValue(in, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
