package com.reservaplus.reserva_plus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Component
public class EspacoTipoColumnMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(EspacoTipoColumnMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;

    public EspacoTipoColumnMigration(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrateIfNeeded() {
        if (!isMySqlFamily()) {
            return;
        }

        String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
        if (schema == null || schema.isBlank()) {
            return;
        }

        List<String> dataTypes = jdbcTemplate.query(
                """
                        SELECT DATA_TYPE
                        FROM information_schema.COLUMNS
                        WHERE TABLE_SCHEMA = ?
                          AND TABLE_NAME = 'espacos'
                          AND COLUMN_NAME = 'tipo'
                        """,
                (resultSet, rowNum) -> resultSet.getString("DATA_TYPE"),
                schema
        );

        if (dataTypes.isEmpty() || !"enum".equalsIgnoreCase(dataTypes.get(0))) {
            return;
        }

        jdbcTemplate.execute("ALTER TABLE espacos MODIFY COLUMN tipo VARCHAR(60) NOT NULL");
        LOGGER.info("Updated espacos.tipo column from enum to varchar.");
    }

    private boolean isMySqlFamily() {
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null
                    && (productName.equalsIgnoreCase("MySQL") || productName.equalsIgnoreCase("MariaDB"));
        }
        catch (SQLException exception) {
            LOGGER.warn("Unable to inspect database metadata for espacos.tipo migration.", exception);
            return false;
        }
    }
}
