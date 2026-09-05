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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        // Modules are now shared across kits (many-to-many via `kit_modules`).
        // The legacy per-kit columns on interview_kit_modules must allow NULL
        // since new module rows no longer write them.
        relaxColumnIfNotNull("interview_kit_modules", "kit_id");
        relaxColumnIfNotNull("interview_kit_modules", "display_order");

        // Questions now belong to a module (which may be shared). kit_id is only
        // retained for kit-scoped "unassigned" questions and is nullable.
        relaxColumnIfNotNull("interview_kit_questions", "kit_id");

        // Backfill kit_modules from the legacy interview_kit_modules rows.
        backfillKitModuleLinks();

        // Ensure order_index is 0-based and contiguous per kit (Hibernate's
        // @OrderColumn list semantics break on 1-based/gapped indices).
        normalizeKitModuleOrder();
    }

    /**
     * Populates the many-to-many junction table `kit_modules` from the legacy
     * per-kit rows in `interview_kit_modules`.
     * Indices are enumerated 0-based per kit. Idempotent: no-op once the
     * junction table already contains links.
     */
    private void backfillKitModuleLinks() {
        try {
            Integer existing = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM kit_modules", Integer.class);
            if (existing != null && existing > 0) {
                return;
            }

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT kit_id, id AS module_id FROM interview_kit_modules"
                            + " WHERE kit_id IS NOT NULL"
                            + " ORDER BY kit_id, COALESCE(display_order, 0)");

            Map<String, Integer> nextIndexByKit = new HashMap<>();
            for (Map<String, Object> row : rows) {
                String kitId = String.valueOf(row.get("kit_id"));
                Object moduleId = row.get("module_id");
                int order = nextIndexByKit.getOrDefault(kitId, 0);
                jdbcTemplate.update(
                        "INSERT INTO kit_modules (kit_id, module_id, order_index) VALUES (?, ?, ?)",
                        kitId, moduleId, order);
                nextIndexByKit.put(kitId, order + 1);
            }

            if (!rows.isEmpty()) {
                log.info("Schema migration: backfilled {} kit_module link(s)", rows.size());
            }
        } catch (Exception e) {
            log.warn("Schema migration: kit_modules backfill skipped: {}", e.getMessage());
        }
    }

    /**
     * Renumbers `kit_modules.order_index` to a 0-based, contiguous sequence per
     * kit. Hibernate's @OrderColumn list mapping requires this (a 1-based or
     * gapped index leaves a null slot in the loaded list and breaks iteration).
     * Idempotent: safe to run on every startup.
     */
    private void normalizeKitModuleOrder() {
        try {
            List<Map<String, Object>> kits = jdbcTemplate.queryForList(
                    "SELECT DISTINCT kit_id FROM kit_modules ORDER BY kit_id");
            int fixed = 0;
            for (Map<String, Object> kitRow : kits) {
                String kitId = String.valueOf(kitRow.get("kit_id"));
                List<Map<String, Object>> mods = jdbcTemplate.queryForList(
                        "SELECT module_id, order_index FROM kit_modules"
                                + " WHERE kit_id = ? ORDER BY order_index", kitId);
                int idx = 0;
                for (Map<String, Object> m : mods) {
                    int current = ((Number) (m.get("order_index") == null ? 0 : m.get("order_index"))).intValue();
                    if (current != idx) {
                        jdbcTemplate.update(
                                "UPDATE kit_modules SET order_index = ? WHERE kit_id = ? AND module_id = ? AND order_index = ?",
                                idx, kitId, String.valueOf(m.get("module_id")), current);
                        fixed++;
                    }
                    idx++;
                }
            }
            if (fixed > 0) {
                log.info("Schema migration: normalized {} kit_modules order row(s)", fixed);
            }
        } catch (Exception e) {
            log.warn("Schema migration: kit_modules order normalization skipped: {}", e.getMessage());
        }
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
