package org.logistix.examples.dispatch.simulation;

import org.logistix.common.enums.PriorityLevel;
import org.logistix.common.model.Coordinates;
import org.logistix.examples.dispatch.model.Certification;
import org.logistix.examples.dispatch.model.DispatchCandidate;
import org.logistix.examples.dispatch.model.Driver;
import org.logistix.examples.dispatch.model.DriverTier;
import org.logistix.examples.dispatch.model.Shipment;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Synthetic scenario generator creating realistic commercial dispatch test beds.
 */
public class DispatchScenarioGenerator {

    private final Random random;

    // Typical logistics hub coordinates (California Logistics Corridor)
    private static final List<Coordinates> HUBS = List.of(
            Coordinates.of(37.7749, -122.4194), // San Francisco
            Coordinates.of(37.8044, -122.2712), // Oakland Hub
            Coordinates.of(37.3382, -121.8863), // San Jose
            Coordinates.of(38.5816, -121.4944), // Sacramento
            Coordinates.of(36.7783, -119.4179), // Fresno
            Coordinates.of(35.3733, -119.0187), // Bakersfield
            Coordinates.of(34.0522, -118.2437), // Los Angeles
            Coordinates.of(33.7701, -118.1937), // Long Beach Port
            Coordinates.of(32.7157, -117.1611)  // San Diego
    );

    public DispatchScenarioGenerator() {
        this(new Random(42));
    }

    public DispatchScenarioGenerator(Random random) {
        this.random = random;
    }

    public List<Driver> generateDrivers(int count) {
        List<Driver> drivers = new ArrayList<>();
        DriverTier[] tiers = DriverTier.values();

        for (int i = 1; i <= count; i++) {
            Coordinates hub = HUBS.get(random.nextInt(HUBS.size()));
            // Add slight jitter +/- 0.15 deg
            double lat = hub.latitude() + (random.nextDouble() - 0.5) * 0.3;
            double lon = hub.longitude() + (random.nextDouble() - 0.5) * 0.3;

            Set<Certification> certs = Collections.emptySet();
            if (i % 3 == 0) certs = Set.of(Certification.HAZMAT);
            else if (i % 4 == 0) certs = Set.of(Certification.REEFER);
            else if (i % 5 == 0) certs = Set.of(Certification.HAZMAT, Certification.TWIC);

            Driver driver = Driver.builder()
                    .driverId(UUID.randomUUID())
                    .name("Fleet-Driver-" + i)
                    .currentLocation(Coordinates.of(lat, lon))
                    .remainingHos(Duration.ofMinutes(240 + random.nextInt(480))) // 4 to 12 hours
                    .vehicleWeightCapacityKg(15000 + (random.nextInt(15) * 1000)) // 15k to 30k kg
                    .vehicleVolumeCapacityM3(40 + (random.nextInt(40))) // 40 to 80 m3
                    .certifications(certs)
                    .tier(tiers[random.nextInt(tiers.length)])
                    .timeUntilMandatoryRest(Duration.ofMinutes(60 + random.nextInt(360)))
                    .homeRegion(i % 2 == 0 ? "US-WEST" : "US-NORTHWEST")
                    .rating(4.0 + (random.nextDouble() * 1.0)) // 4.0 - 5.0
                    .historicalOnTimeRate(0.85 + (random.nextDouble() * 0.14)) // 85% - 99%
                    .build();

            drivers.add(driver);
        }
        return drivers;
    }

    public Shipment generateShipment(Instant now, boolean requireHazmat) {
        Coordinates origin = HUBS.get(random.nextInt(HUBS.size()));
        Coordinates dest;
        do {
            dest = HUBS.get(random.nextInt(HUBS.size()));
        } while (dest.equals(origin));

        Set<Certification> certs = requireHazmat ? Set.of(Certification.HAZMAT) : Collections.emptySet();

        return Shipment.builder()
                .shipmentId(UUID.randomUUID())
                .origin(origin)
                .destination(dest)
                .weightKg(3000 + random.nextInt(18000))
                .volumeM3(10 + random.nextInt(45))
                .requiredCertifications(certs)
                .pickupTimeWindowStart(now)
                .pickupTimeWindowEnd(now.plusSeconds(3600))
                .deliveryDeadline(now.plusSeconds(28800 + random.nextInt(14400))) // 8 to 12 hours
                .priority(PriorityLevel.HIGH)
                .destinationRegion("US-WEST")
                .build();
    }

    public List<DispatchCandidate> buildCandidatePairings(List<Driver> drivers, Shipment shipment, Instant now) {
        List<DispatchCandidate> list = new ArrayList<>();
        for (Driver driver : drivers) {
            list.add(DispatchCandidate.from(driver, shipment, now, 0.20, "CLEAR"));
        }
        return list;
    }
}
