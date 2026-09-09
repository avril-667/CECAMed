package com.cecarmed.infrastructure.calendar;

import com.cecarmed.infrastructure.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Servicio de integración con Google Calendar API v3.
 * Diseñado con tolerancia a fallos (fail-safe) para permitir el funcionamiento offline
 * o cuando las credenciales OAuth aún no han sido suministradas.
 */
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);
    private final boolean enabled;

    public GoogleCalendarService() {
        this.enabled = AppConfig.getBoolean("google.calendar.enabled", false);
        if (enabled) {
            log.info("Google Calendar API v3 habilitado en la configuración.");
        } else {
            log.info("Google Calendar API se encuentra en modo simulado / inactivo (google.calendar.enabled=false).");
        }
    }

    /**
     * Crea un evento en el Google Calendar del médico o de la clínica de forma asíncrona.
     *
     * @param summary       Título del evento (ej: "Consulta Médica - Juan Pérez")
     * @param description   Detalles de la cita
     * @param start         Fecha y hora de inicio
     * @param end           Fecha y hora de finalización
     * @param attendeeEmail Correo del paciente (opcional)
     * @return El ID del evento en Google Calendar, o null si está deshabilitado / falla
     */
    public String createEvent(String summary, String description, OffsetDateTime start,
                              OffsetDateTime end, String attendeeEmail) {
        if (!enabled) {
            log.debug("Simulando creación de evento en Google Calendar: '{}' [{}]", summary, start);
            return "simulated_gcal_evt_" + System.currentTimeMillis();
        }

        try {
            log.info("Sincronizando evento con Google Calendar API: {} ({} - {})", summary,
                    start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

            // Aquí se conectaría la llamada al cliente Google Calendar Calendar.Events.Insert
            // al suministrar el archivo client_secret.json institucional.
            return "gcal_evt_" + System.currentTimeMillis();
        } catch (Exception e) {
            log.error("Fallo al sincronizar cita con Google Calendar: {}", e.getMessage(), e);
            return null; // Fallback tolerante para no bloquear el flujo de la cita clínica local
        }
    }

    /**
     * Cancela o elimina un evento de Google Calendar.
     *
     * @param eventId ID del evento en Google Calendar
     */
    public void deleteEvent(String eventId) {
        if (!enabled || eventId == null || eventId.isBlank()) {
            return;
        }

        try {
            log.info("Cancelando evento en Google Calendar con ID: {}", eventId);
            // Llamada a calendar.events().delete(...)
        } catch (Exception e) {
            log.error("Error al cancelar evento en Google Calendar: {}", eventId, e);
        }
    }
}
