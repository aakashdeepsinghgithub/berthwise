package com.berthwise.service;

/** Invoice lines in paise. */
public record Invoice(String bookingId,
                      long portDuesPaise,
                      long berthHirePaise,
                      long demurragePaise,
                      long gstPaise,
                      long totalPaise) {

    public long subtotalPaise() {
        return portDuesPaise + berthHirePaise + demurragePaise;
    }
}
