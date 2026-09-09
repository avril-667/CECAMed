package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.PacienteService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

public class CitaDialogController {

    private static final Logger log = LoggerFactory.getLogger(CitaDialogController.class);

    @FXML private ComboBox<Paciente> cbPaciente;
    @FXML private ComboBox<Usuario> cbMedico;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<LocalTime> cbHoraInicio;
    @FXML private ComboBox<Integer> cbDuracion;
    @FXML private TextField txtMotivo;
    @FXML private TextArea txtNotas;
    @FXML private Label lblError;
    @FXML private Button btnAgendar;

    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;

    private Stage dialogStage;
    private boolean agendadaExitosa = false;

    public CitaDialogController(CitaService citaService, PacienteService pacienteService, UsuarioRepository usuarioRepository) {
        this.citaService = citaService;
        this.pacienteService = pacienteService;
        this.usuarioRepository = usuarioRepository;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    public void initialize() {
        hideError();
        dpFecha.setValue(LocalDate.now());

        // Opciones de duración: 15, 30, 45, 60 minutos
        cbDuracion.getItems().addAll(15, 30, 45, 60);
        cbDuracion.setValue(30);

        // Opciones de horas de inicio cada 30 minutos (07:00 a 20:00)
        LocalTime time = LocalTime.of(7, 0);
        while (!time.isAfter(LocalTime.of(20, 0))) {
            cbHoraInicio.getItems().add(time);
            time = time.plusMinutes(30);
        }
        cbHoraInicio.setValue(LocalTime.of(9, 0));

        configurarConversores();
        cargarDatosAsincronos();
    }

    private void configurarConversores() {
        cbPaciente.setConverter(new StringConverter<>() {
            @Override
            public String toString(Paciente p) {
                return p == null ? "" : p.expedienteNumero() + " - " + p.getNombreCompleto();
            }

            @Override
            public Paciente fromString(String string) {
                return null;
            }
        });

        cbMedico.setConverter(new StringConverter<>() {
            @Override
            public String toString(Usuario u) {
                return u == null ? "" : "Dr(a). " + u.nombreCompleto() + " (" + (u.especialidad() != null ? u.especialidad() : "Medicina General") + ")";
            }

            @Override
            public Usuario fromString(String string) {
                return null;
            }
        });
    }

    private void cargarDatosAsincronos() {
        Thread.ofVirtual().start(() -> {
            try {
                List<Paciente> pacientes = pacienteService.buscarPacientes(null, 500, 1);
                List<Usuario> medicos = usuarioRepository.findByRol(Rol.MEDICO);

                Platform.runLater(() -> {
                    cbPaciente.getItems().setAll(pacientes);
                    if (!pacientes.isEmpty()) cbPaciente.getSelectionModel().selectFirst();

                    cbMedico.getItems().setAll(medicos);
                    if (!medicos.isEmpty()) cbMedico.getSelectionModel().selectFirst();
                });
            } catch (Exception e) {
                log.error("Error al cargar pacientes o médicos para diálogo de citas", e);
            }
        });
    }

    @FXML
    public void handleAgendar() {
        hideError();

        Paciente paciente = cbPaciente.getValue();
        Usuario medico = cbMedico.getValue();
        LocalDate fecha = dpFecha.getValue();
        LocalTime horaInicio = cbHoraInicio.getValue();
        Integer duracion = cbDuracion.getValue();
        String motivo = txtMotivo.getText();
        String notas = txtNotas.getText();

        if (paciente == null || medico == null || fecha == null || horaInicio == null) {
            showError("Todos los campos obligatorios deben ser completados.");
            return;
        }

        LocalTime horaFin = horaInicio.plusMinutes(duracion != null ? duracion : 30);
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime startDateTime = fecha.atTime(horaInicio).atZone(zone).toOffsetDateTime();
        OffsetDateTime endDateTime = fecha.atTime(horaFin).atZone(zone).toOffsetDateTime();

        Cita nueva = Cita.nueva(paciente.id(), medico.id(), startDateTime, endDateTime, motivo, notas);
        btnAgendar.setDisable(true);

        Thread.ofVirtual().start(() -> {
            try {
                citaService.agendarCita(nueva, paciente.getNombreCompleto(), paciente.email());
                Platform.runLater(() -> {
                    agendadaExitosa = true;
                    dialogStage.close();
                });
            } catch (IllegalStateException | IllegalArgumentException e) {
                Platform.runLater(() -> {
                    btnAgendar.setDisable(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                log.error("Error inesperado al agendar cita", e);
                Platform.runLater(() -> {
                    btnAgendar.setDisable(false);
                    showError("Error del sistema al programar la cita.");
                });
            }
        });
    }

    @FXML
    public void handleCancelar() {
        dialogStage.close();
    }

    public boolean isAgendadaExitosa() {
        return agendadaExitosa;
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void hideError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }
}
