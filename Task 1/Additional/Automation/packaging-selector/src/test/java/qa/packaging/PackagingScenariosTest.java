package qa.packaging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data-driven black-box tests. One scenario = one test case from the spreadsheet (same IDs).
 * Groups mirror the checklist sections.
 */
class PackagingScenariosTest {

    private final PackagingSelector selector = new PackagingSelector();

    static Stream<Scenario> shape() {
        return Scenarios.group("SHAPE").stream();
    }

    static Stream<Scenario> filters() {
        return Scenarios.group("FILTER").stream();
    }

    static Stream<Scenario> box() {
        return Scenarios.group("BOX").stream();
    }

    static Stream<Scenario> bag() {
        return Scenarios.group("BAG").stream();
    }

    static Stream<Scenario> choice() {
        return Scenarios.group("CHOICE").stream();
    }

    static Stream<Scenario> invalid() {
        return Scenarios.group("INVALID").stream();
    }

    @DisplayName("Shipment shape")
    @ParameterizedTest(name = "{0}")
    @MethodSource("shape")
    void shipmentShape(Scenario scenario) {
        verify(scenario);
    }

    @DisplayName("Availability and weight filters")
    @ParameterizedTest(name = "{0}")
    @MethodSource("filters")
    void filters(Scenario scenario) {
        verify(scenario);
    }

    @DisplayName("Box dimension check")
    @ParameterizedTest(name = "{0}")
    @MethodSource("box")
    void boxFit(Scenario scenario) {
        verify(scenario);
    }

    @DisplayName("Bag dimension check")
    @ParameterizedTest(name = "{0}")
    @MethodSource("bag")
    void bagFit(Scenario scenario) {
        verify(scenario);
    }

    @DisplayName("Choosing the cheapest")
    @ParameterizedTest(name = "{0}")
    @MethodSource("choice")
    void cheapestChoice(Scenario scenario) {
        verify(scenario);
    }

    @DisplayName("Invalid input")
    @ParameterizedTest(name = "{0}")
    @MethodSource("invalid")
    void invalidInput(Scenario scenario) {
        verify(scenario);
    }

    private void verify(Scenario scenario) {
        assertThat(ScenarioRunner.outcomeOf(selector, scenario))
                .as("%s (%s)", scenario.id(), scenario.technique())
                .isEqualTo(scenario.expected().asText());
    }
}
