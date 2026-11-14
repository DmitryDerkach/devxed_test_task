package qa.packaging;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/** Loads {@code /scenarios.json} from the test classpath. */
public final class Scenarios {

    private static final List<Scenario> ALL = load();

    private Scenarios() {
    }

    public static List<Scenario> all() {
        return ALL;
    }

    public static List<Scenario> group(String group) {
        return ALL.stream().filter(s -> s.group().equals(group)).toList();
    }

    private static List<Scenario> load() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = Scenarios.class.getResourceAsStream("/scenarios.json")) {
            if (in == null) {
                throw new IllegalStateException("scenarios.json not found on the test classpath");
            }
            return mapper.readValue(in, new TypeReference<>() {
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
