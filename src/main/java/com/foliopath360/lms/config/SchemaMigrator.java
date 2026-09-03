package com.foliopath360.lms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Idempotent schema migrations that run on startup.
 * Kept lightweight and self-contained (no Flyway/Liquibase dependency).
 */
@Component
public class SchemaMigrator implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(SchemaMigrator.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public SchemaMigrator(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        // order_items.course_id & course_title must allow NULL so that
        // interview-kit purchases (which use kit_id/kit_name) can be stored.
        relaxColumnIfNotNull("order_items", "course_id");
        relaxColumnIfNotNull("order_items", "course_title");
    }

    /**
     * Drops the NOT NULL constraint on a column if it is currently NOT NULL.
     * Safe to run every startup (no-op once the column is nullable).
     */
    private void relaxColumnIfNotNull(String table, String column) throws Exception {
        Connection conn = dataSource.getConnection();
        try {
            DatabaseMetaData meta = conn.getMetaData();

            String colType = null;
            boolean isNotNull = false;
            try (ResultSet rs = meta.getColumns(conn.getCatalog(), null, table, column)) {
                if (rs.next()) {
                    isNotNull = "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"));
                    colType = buildColumnType(
                            rs.getString("TYPE_NAME"),
                            rs.getString("COLUMN_SIZE"),
                            rs.getString("DECIMAL_DIGITS")
                    );
                }
            }

            if (!isNotNull || colType == null) {
                return;
            }

            String sql = "ALTER TABLE " + table + " MODIFY COLUMN " + column
                    + " " + colType + " NULL";
            jdbcTemplate.execute(sql);
            log.info("Schema migration: relaxed NOT NULL on {}.{}", table, column);
        } finally {
            conn.close();
        }
    }

    private String buildColumnType(String dataType, String size, String decimals) {
        String type = dataType == null ? "VARCHAR" : dataType;
        if ("VARCHAR".equalsIgnoreCase(type)
                || "CHAR".equalsIgnoreCase(type)) {
            return type + "(" + (size == null ? "200" : size) + ")";
        }
        if ("DECIMAL".equalsIgnoreCase(type)
                || "NUMERIC".equalsIgnoreCase(type)) {
            int s = size == null ? 10 : Integer.parseInt(size);
            int d = decimals == null ? 2 : Integer.parseInt(decimals);
            return type + "(" + s + "," + d + ")";
        }
        return type;
    }
}
