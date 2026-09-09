package com.cecarmed.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Gestión centralizada de configuración de la aplicación CECAMed.
 * Lee desde application.properties y permite sobreescritura mediante variables de entorno o propiedades de sistema.
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                properties.load(is);
                log.info("Archivo application.properties cargado exitosamente.");
            } else {
                log.warn("No se encontró application.properties en el classpath. Usando valores predeterminados.");
            }
        } catch (IOException e) {
            log.error("Error al cargar application.properties: {}", e.getMessage(), e);
        }
    }

    private AppConfig() {
        // Utility class
    }

    public static String get(String key, String defaultValue) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }

        String sysVal = System.getProperty(key);
        if (sysVal != null && !sysVal.isBlank()) {
            return sysVal;
        }

        return properties.getProperty(key, defaultValue);
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key, null);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            log.warn("Valor inválido para {}: '{}'. Usando valor por defecto: {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key, null);
        if (val == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(val.trim());
    }

    public static String getJdbcUrl() {
        String host = get("db.host", "localhost");
        int port = getInt("db.port", 5432);
        String dbName = get("db.name", "cecarmed_db");
        return String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
    }

    public static String getDbUser() {
        return get("db.username", "postgres");
    }

    public static String getDbPassword() {
        return get("db.password", "postgres");
    }
}
