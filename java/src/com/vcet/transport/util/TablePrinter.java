package com.vcet.transport.util;

import java.util.*;

/** Renders rows as an aligned text table using StringBuilder. */
public final class TablePrinter {
    private TablePrinter() { }

    public static String render(String title, List<String> headers, List<List<String>> rows) {
        int cols = headers.size();
        int[] w = new int[cols];
        for (int i = 0; i < cols; i++) w[i] = headers.get(i).length();
        for (List<String> r : rows)
            for (int i = 0; i < cols; i++) w[i] = Math.max(w[i], r.get(i).length());

        StringBuilder line = new StringBuilder("+");
        for (int x : w) line.append("-".repeat(x + 2)).append('+');

        StringBuilder sb = new StringBuilder();
        if (title != null) sb.append("\n== ").append(title).append(" ==\n");
        sb.append(line).append('\n').append(row(headers, w)).append(line).append('\n');
        for (List<String> r : rows) sb.append(row(r, w));
        sb.append(line).append('\n').append(rows.size()).append(" row(s)\n");
        return sb.toString();
    }

    private static String row(List<String> cells, int[] w) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < w.length; i++) sb.append(' ').append(String.format("%-" + w[i] + "s", cells.get(i))).append(" |");
        return sb.append('\n').toString();
    }
}
