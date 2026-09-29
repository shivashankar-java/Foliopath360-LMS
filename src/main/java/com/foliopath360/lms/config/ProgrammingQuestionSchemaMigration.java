package com.foliopath360.lms.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Programming questions used to be owned by a course
 * ({@code programming_questions.course_id NOT NULL}). They are now a
 * standalone bank, so the entity no longer maps that column. Hibernate's
 * {@code ddl-auto=update} never drops columns, which would leave the leftover
 * NOT NULL constraint blocking every insert. Relax it once, on startup.
 */
@Component
public class ProgrammingQuestionSchemaMigration implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(ProgrammingQuestionSchemaMigration.class);

    private static final String TABLE = "programming_questions";
    private static final String COLUMN = "course_id";

    private final DataSource dataSource;

    public ProgrammingQuestionSchemaMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            if (!columnExists(connection)) {
                return;
            }
            if (isNullable(connection)) {
                return;
            }

            statement.execute(
                    "ALTER TABLE " + TABLE + " MODIFY COLUMN " + COLUMN + " VARCHAR(36) NULL");
            log.info("Relaxed {}.{} to nullable — programming questions are course-independent",
                    TABLE, COLUMN);
        } catch (Exception ex) {
            log.warn("Could not relax {}.{}; programming question inserts may fail", TABLE, COLUMN, ex);
        }
    }

    private boolean columnExists(Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(
                connection.getCatalog(), null, TABLE, COLUMN)) {
            return columns.next();
        }
    }

    private boolean isNullable(Connection connection) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(
                connection.getCatalog(), null, TABLE, COLUMN)) {
            if (!columns.next()) {
                return true;
            }
            return "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"));
        }
    }
}
