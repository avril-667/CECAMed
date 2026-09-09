package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.PacienteService;
import com.cecarmed.service.SalaEsperaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CheckInDialogController {

    private static final Logger log = LoggerFactory.getLogger(CheckInDialogController.class);

    @FXML private ComboBox<CitaItem> cbCitasHoy;
    @FXML private TextField txtObservaciones;
    @FXML private Label lblError;
    @FXML private Button btnCheckIn;

    private final SalaEsperaService salaEsperaService;
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;

    private Stage dialogStage;
    private boolean checkInExitoso = false;

    public record CitaItem(Cita cita, String display) {}

    public CheckInDialogController(SalaEsperaService salaEsperaService,
                                   CitaService citaService,
                                   PacienteService pacienteService,
                                   UsuarioRepository usuarioRepository) {
        this.salaEsperaService = salaEsperaService;
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
        cbCitasHoy.setConverter(new StringConverter<>() {
            @Override
            public String toString(CitaItem item) {
                return item == null ? "" : item.display();
            }
            @Override
            public CitaItem fromString(String string) { return null; }
        });

        cargarCitasPendientes();
    }

    private void cargarCitasPendientes() {
        Thread.ofVirtual().start(() -> {
            try {
                List<Cita> citas = citaService.obtenerCitasPorFecha(LocalDate.now());
                DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
                List<CitaItem> items = new ArrayList<>();

                for (Cita c : citas) {
                    if (c.estado() == EstadoCita.PROGRAMADA || c.estado() == EstadoCita.CONFIRMADA) {
                        Optional<Paciente> p = pacienteService.obtenerPorId(c.pacienteId());
                        Optional<Usuario> m = usuarioRepository.findById(c.medicoId());

                        String pac = p.map(Paciente::getNombreCompleto).orElse("Paciente");
                        String med = m.map(u -> "Dr(a). " + u.nombreCompleto()).orElse("Médico");
                        String hora = c.fechaHoraInicio().format(timeFmt);

                        String label = String.format("[%s] %s con %s", hora, pac, med);
                        items.add(new CitaItem(c, label));
                    }
                }

                Platform.runLater(() -> {
                    cbCitasHoy.getItems().setAll(items);
                    if (!items.isEmpty()) {
                        cbCitasHoy.getSelectionModel().selectFirst();
                    } else {
                        showError("No hay citas programadas pendientes de check-in para el día de hoy.");
                        btnCheckIn.setDisable(true);
                    }
                });
            } catch (Exception e) {
                log.error("Error al cargar citas para check-in", e);
            }
        });
    }

    @FXML
    public void handleCheckIn() {
        hideError();
        CitaItem selected = cbCitasHoy.getValue();
        if (selected == null) {
            showError("Por favor, selecciona una cita de la lista.");
            return;
        }

        String obs = txtObservaciones.getText();
        btnCheckIn.setDisable(true);

        Thread.ofVirtual().start(() -> {
            try {
                salaEsperaService.registrarLlegada(selected.cita().id(), obs);
                Platform.runLater(() -> {
                    checkInExitoso = true;
                    dialogStage.close();
                });
            } catch (IllegalStateException | IllegalArgumentException e) {
                Platform.runLater(() -> {
                    btnCheckIn.setDisable(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                log.error("Error en proceso de check-in", e);
                Platform.runLater(() -> {
                    btnCheckIn.setDisable(false);
                    showError("Error al registrar la llegada en la sala de espera.");
                });
            }
        });
    }

    @FXML
    public void handleCancelar() {
        dialogStage.close();
    }

    public boolean isCheckInExitoso() {
        return checkInExitoso;
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
