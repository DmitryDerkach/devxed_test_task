package qa.packaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the scenarios are able to catch bugs: every mutant must fail at least one scenario,
 * while the real implementation must fail none.
 */
class MutationSanityTest {

    static Stream<Mutants.Mutant> mutants() {
        return Mutants.all().stream();
    }

    @Test
    @DisplayName("baseline: the real implementation passes every scenario")
    void baselinePassesEverything() {
        PackagingSelector real = new PackagingSelector();

        List<String> failing = Scenarios.all().stream()
                .filter(s -> !ScenarioRunner.passes(real, s))
                .map(Scenario::id)
                .toList();

        assertThat(failing).isEmpty();
    }

    @DisplayName("mutant is killed by at least one scenario")
    @ParameterizedTest(name = "{0}")
    @MethodSource("mutants")
    void mutantIsKilled(Mutants.Mutant mutant) {
        List<String> killers = Scenarios.all().stream()
                .filter(s -> !ScenarioRunner.passes(mutant.selector(), s))
                .map(Scenario::id)
                .toList();

        assertThat(killers)
                .as("scenarios that detect mutant %s", mutant)
                .isNotEmpty();
    }
}
