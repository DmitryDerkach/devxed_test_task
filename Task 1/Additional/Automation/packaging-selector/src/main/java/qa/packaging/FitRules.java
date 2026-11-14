package qa.packaging;

/** Strategy: is this packaging suitable for this shipment? */
@FunctionalInterface
public interface FitRules {

    FitRules STANDARD = new StandardFitRules();

    boolean isSuitable(Shipment shipment, Packaging packaging);
}
