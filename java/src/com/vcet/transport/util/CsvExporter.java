package com.vcet.transport.util;

import com.vcet.transport.db.DBConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.sql.*;

/** Dumps a SQL query to CSV (used to hand data over to the Python analytics step). */
public final class CsvExporter {
    private CsvExporter() { }

    public static int export(String sql, Path file) throws SQLException, IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        int rows = 0;
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql);
             PrintWriter out = new PrintWriter(Files.newBufferedWriter(file))) {
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            StringBuilder h = new StringBuilder();
            for (int i = 1; i <= n; i++) h.append(i > 1 ? "," : "").append(md.getColumnLabel(i));
            out.println(h);
            while (rs.next()) {
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= n; i++) {
                    String v = rs.getString(i);
                    if (i > 1) sb.append(',');
                    if (v != null) sb.append('"').append(v.replace("\"", "\"\"")).append('"');
                }
                out.println(sb);
                rows++;
            }
        }
        return rows;
    }
}
