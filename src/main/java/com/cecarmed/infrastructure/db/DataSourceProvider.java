package com.cecarmed.infrastructure.db;

import com.cecarmed.infrastructure.config.AppConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Proveedor y gestor centralizado del pool de conexiones HikariCP y migraciones con Flyway.
 */
public final class DataSourceProvider {

    private static final Logger log = LoggerFactory.getLogger(DataSourceProvider.class);
    private static volatile HikariDataSource dataSource;

    private DataSourceProvider() {
    }

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DataSourceProvider.class) {
                if (dataSource == null) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    private static void initDataSource() {
        log.info("Inicializando DataSource HikariCP hacia: {}", AppConfig.getJdbcUrl());
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.getJdbcUrl());
        config.setUsername(AppConfig.getDbUser());
        config.setPassword(AppConfig.getDbPassword());
        config.setMaximumPoolSize(AppConfig.getInt("hikari.maximum-pool-size", 10));
        config.setMinimumIdle(AppConfig.getInt("hikari.minimum-idle", 2));
        config.setIdleTimeout(AppConfig.getInt("hikari.idle-timeout-ms", 30000));
        config.setConnectionTimeout(AppConfig.getInt("hikari.connection-timeout-ms", 20000));
        config.setMaxLifetime(AppConfig.getInt("hikari.max-lifetime-ms", 1800000));
        config.setPoolName("CECAMed-HikariPool");

        // Optimizaciones estándar para PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);
        log.info("HikariCP inicializado correctamente.");

        if (AppConfig.getBoolean("flyway.enabled", true)) {
            runFlywayMigrations(dataSource);
        }
    }

    public static void runFlywayMigrations(DataSource ds) {
        log.info("Ejecutando migraciones de Flyway...");
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(ds)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(AppConfig.getBoolean("flyway.baseline-on-migrate", true))
                    .load();

            var result = flyway.migrate();
            log.info("Migraciones de Flyway ejecutadas exitosamente. Versiones aplicadas: {}", result.migrationsExecuted);
        } catch (Exception e) {
            log.error("Fallo al ejecutar las migraciones de Flyway: {}", e.getMessage(), e);
            throw new RuntimeException("Error fatal inicializando la base de datos", e);
        }
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Cerrando el pool de conexiones HikariCP...");
            dataSource.close();
        }
    }
}
