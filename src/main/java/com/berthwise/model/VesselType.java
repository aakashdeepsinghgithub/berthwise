package com.berthwise.model;

/** Vessel categories with their hourly berth-hire rate in paise. */
public enum VesselType {
    CONTAINER(180_000),
    BULK(120_000),
    TANKER(210_000),
    RORO(150_000),
    CRUISE(250_000);

    private final int hourlyRatePaise;

    VesselType(int hourlyRatePaise) {
        this.hourlyRatePaise = hourlyRatePaise;
    }

    public int getHourlyRatePaise() {
        return hourlyRatePaise;
    }
}
