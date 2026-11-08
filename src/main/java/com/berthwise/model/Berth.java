package com.berthwise.model;

import java.util.Objects;
import java.util.Set;

/** A physical berth along the quay. */
public final class Berth {

    private final String id;
    private final String name;
    private final double lengthMeters;
    private final double depthAtDatumMeters;
    private final Set<VesselType> allowedTypes;

    public Berth(String id, String name, double lengthMeters, double depthAtDatumMeters,
                 Set<VesselType> allowedTypes) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        if (lengthMeters <= 0 || depthAtDatumMeters <= 0) {
            throw new IllegalArgumentException("Berth dimensions must be positive for " + id);
        }
        if (allowedTypes == null || allowedTypes.isEmpty()) {
            throw new IllegalArgumentException("Berth " + id + " must allow at least one vessel type");
        }
        this.lengthMeters = lengthMeters;
        this.depthAtDatumMeters = depthAtDatumMeters;
        this.allowedTypes = Set.copyOf(allowedTypes);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public double getLengthMeters() { return lengthMeters; }
    public double getDepthAtDatumMeters() { return depthAtDatumMeters; }
    public Set<VesselType> getAllowedTypes() { return allowedTypes; }

    public boolean accepts(VesselType type) {
        return allowedTypes.contains(type);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Berth && ((Berth) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id + " " + name;
    }
}
