package com.vcet.transport.util;

import com.vcet.transport.exception.ValidationException;
import java.util.regex.Pattern;

/** Input validation helpers (String handling + regular expressions). */
public final class Validator {
    private static final Pattern BUS_NO = Pattern.compile("^[A-Z0-9][A-Z0-9-]{4,19}$");
    private static final Pattern PHONE  = Pattern.compile("^[6-9]\\d{9}$");
    private static final Pattern EMAIL  = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PLACE  = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,49}$");
    private static final Pattern NAME   = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,79}$");

    private Validator() { }

    public static String busNumber(String s) throws ValidationException {
        String v = s == null ? "" : s.trim().toUpperCase();
        if (!BUS_NO.matcher(v).matches())
            throw new ValidationException("Invalid bus number (use 5-20 letters/digits/hyphens, e.g. TN58-AC-1001)");
        return v;
    }
    public static int capacity(int c) throws ValidationException {
        if (c < 1 || c > 80) throw new ValidationException("Capacity must be between 1 and 80");
        return c;
    }
    /** Normalises to Title Case: "  mADURAI " -> "Madurai". */
    public static String place(String s) throws ValidationException {
        String v = s == null ? "" : s.trim().replaceAll("\\s+", " ");
        if (!PLACE.matcher(v).matches()) throw new ValidationException("Invalid place name: '" + s + "'");
        StringBuilder sb = new StringBuilder();
        for (String w : v.split(" ")) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
    }
    public static double distance(double d) throws ValidationException {
        if (d <= 0 || d > 5000) throw new ValidationException("Distance must be > 0 and <= 5000 km");
        return d;
    }
    public static String name(String s) throws ValidationException {
        String v = s == null ? "" : s.trim().replaceAll("\\s+", " ");
        if (!NAME.matcher(v).matches()) throw new ValidationException("Invalid passenger name");
        return v;
    }
    public static String phone(String s) throws ValidationException {
        String v = s == null ? "" : s.trim();
        if (!PHONE.matcher(v).matches()) throw new ValidationException("Phone must be a 10-digit Indian mobile number");
        return v;
    }
    /** Optional: blank -> null. */
    public static String emailOrNull(String s) throws ValidationException {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().toLowerCase();
        if (!EMAIL.matcher(v).matches()) throw new ValidationException("Invalid e-mail address");
        return v;
    }
    public static Integer ageOrNull(String s) throws ValidationException {
        if (s == null || s.isBlank()) return null;
        try {
            int a = Integer.parseInt(s.trim());
            if (a < 1 || a > 110) throw new ValidationException("Age must be between 1 and 110");
            return a;
        } catch (NumberFormatException e) { throw new ValidationException("Age must be a number"); }
    }
}
