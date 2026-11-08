package com.berthwise.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A ship calling at the port. Identity is the IMO number; names can change over a ship's life.
 */
public final class Vessel {

    private static final Pattern IMO_PATTERN = Pattern.compile("IMO\\d{7}");

    private final String imo;
    private final String name;
    private final VesselType type;
    private final double loaMeters;
    private final double beamMeters;
    private final double draftMeters;
    private final int grossTonnage;

    public Vessel(String imo, String name, VesselType type,
                  double loaMeters, double beamMeters, double draftMeters, int grossTonnage) {
        if (imo == null || !IMO_PATTERN.matcher(imo).matches()) {
            throw new IllegalArgumentException("Invalid IMO number: " + imo);
        }
        if (loaMeters <= 0 || beamMeters <= 0 || draftMeters <= 0 || grossTonnage <= 0) {
            throw new IllegalArgumentException("Vessel dimensions must be positive for " + imo);
        }
        this.imo = imo;
        this.name = Objects.requireNonNull(name, "name");
        this.type = Objects.requireNonNull(type, "type");
        this.loaMeters = loaMeters;
        this.beamMeters = beamMeters;
        this.draftMeters = draftMeters;
        this.grossTonnage = grossTonnage;
    }

    public String getImo() { return imo; }
    public String getName() { return name; }
    public VesselType getType() { return type; }
    public double getLoaMeters() { return loaMeters; }
    public double getBeamMeters() { return beamMeters; }
    public double getDraftMeters() { return draftMeters; }
    public int getGrossTonnage() { return grossTonnage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vessel)) return false;
        Vessel other = (Vessel) o;
        return imo.equals(other.imo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(imo, name);
    }

    @Override
    public String toString() {
        return name + " (" + imo + ", " + type + ")";
    }
}
