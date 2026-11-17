package com.vcet.transport.model;

public enum BusType {
    AC_SLEEPER("AC Sleeper"), AC_SEATER("AC Seater"),
    NON_AC_SEATER("Non-AC Seater"), DELUXE("Deluxe");

    private final String label;
    BusType(String label) { this.label = label; }
    public String label() { return label; }

    /** Accepts enum name or a menu number 1-4. */
    public static BusType parse(String s) {
        String t = s.trim().toUpperCase().replace(' ', '_').replace('-', '_');
        switch (t) {
            case "1": return AC_SLEEPER;
            case "2": return AC_SEATER;
            case "3": return NON_AC_SEATER;
            case "4": return DELUXE;
            default:  return BusType.valueOf(t);
        }
    }
}
