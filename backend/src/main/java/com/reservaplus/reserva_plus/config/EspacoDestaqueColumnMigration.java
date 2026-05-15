package com.reservaplus.reserva_plus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

@Component
public class EspacoDestaqueColumnMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EspacoDestaqueColumnMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public EspacoDestaqueColumnMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateIfNeeded() {
        try (Connection connection = dataSource.getConnection()) {
            if (hasColumn(connection, "espacos", "destaque")) {
                return;
            }
        }
        catch (SQLException exception) {
            LOGGER.warn("Unable to inspect database metadata for espacos.destaque migration.", exception);
            return;
        }

        jdbcTemplate.execute("ALTER TABLE espacos ADD COLUMN destaque BOOLEAN NOT NULL DEFAULT FALSE");
        LOGGER.info("Added espacos.destaque column.");
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        String catalog = connection.getCatalog();
        String schema = connection.getSchema();

        for (String tableCandidate : identifierCandidates(tableName)) {
            for (String columnCandidate : identifierCandidates(columnName)) {
                try (ResultSet columns = metaData.getColumns(catalog, schema, tableCandidate, columnCandidate)) {
                    if (columns.next()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private List<String> identifierCandidates(String value) {
        return List.of(
                value,
                value.toLowerCase(Locale.ROOT),
                value.toUpperCase(Locale.ROOT)
        );
    }
}
