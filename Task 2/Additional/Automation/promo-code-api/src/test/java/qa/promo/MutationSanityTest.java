package qa.promo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the API cases are able to catch bugs: the real service passes every case,
 * every mutant fails at least one.
 */
class MutationSanityTest {

    static Stream<PromoMutants.Mutant> mutants() {
        return PromoMutants.all().stream();
    }

    private static List<String> failingCases(PromoService service) throws IOException {
        try (PromoApplication app = PromoApplication.start(service, 0)) {
            return ApiCases.all().stream()
                    .filter(c -> !ApiCaseRunner.mismatches(app.baseUri(), c).isEmpty())
                    .map(ApiCase::id)
                    .toList();
        }
    }

    @Test
    @DisplayName("baseline: the real service passes every case")
    void baselinePassesEverything() throws IOException {
        assertThat(failingCases(new PromoService(new InMemoryPromoCodeRepository()))).isEmpty();
    }

    @DisplayName("mutant is killed by at least one case")
    @ParameterizedTest(name = "{0}")
    @MethodSource("mutants")
    void mutantIsKilled(PromoMutants.Mutant mutant) throws IOException {
        assertThat(failingCases(mutant.service()))
                .as("cases that detect mutant %s", mutant)
                .isNotEmpty();
    }
}
