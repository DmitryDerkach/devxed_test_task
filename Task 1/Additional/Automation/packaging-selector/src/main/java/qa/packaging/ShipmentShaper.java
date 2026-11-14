package qa.packaging;

import java.util.List;

/** Strategy: how a list of items becomes one {@link Shipment}. Pluggable so tests can swap in mutants. */
@FunctionalInterface
public interface ShipmentShaper {

    ShipmentShaper STANDARD = Shipment::of;

    Shipment shape(List<Item> items);
}
