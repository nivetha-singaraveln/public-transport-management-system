package com.vcet.transport.util;

import com.vcet.transport.exception.*;
import java.sql.SQLException;

/** Translates low-level SQLExceptions into meaningful business exceptions. */
public final class SqlErrors {
    private SqlErrors() { }

    public static TransportException translate(SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String lower = msg.toLowerCase();
        if ("45000".equals(e.getSQLState())) {              // raised by our triggers
            if (lower.contains("full"))     return new BusFullException(msg);
            if (lower.contains("capacity")) return new SeatUnavailableException(msg);
            return new ValidationException(msg);
        }
        if (e.getErrorCode() == 1062 || lower.contains("duplicate entry")) {
            return new ValidationException("Duplicate record: value already exists (" + shorten(msg) + ")");
        }
        if (e.getErrorCode() == 1451) {
            return new ValidationException("Cannot delete: record is referenced by other data");
        }
        if (e.getErrorCode() == 1452) {
            return new ValidationException("Referenced record does not exist");
        }
        if (e.getErrorCode() == 4025 || lower.contains("constraint")) {
            return new ValidationException("Value violates a database rule: " + shorten(msg));
        }
        return new TransportException("Database error: " + shorten(msg), e);
    }

    private static String shorten(String s) {
        int nl = s.indexOf('\n');
        String first = nl > 0 ? s.substring(0, nl) : s;
        return first.length() > 110 ? first.substring(0, 110) + "..." : first;
    }
}
