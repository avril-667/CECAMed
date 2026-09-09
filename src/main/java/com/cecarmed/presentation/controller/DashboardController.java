package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.session.UserSession;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.PacienteService;
import com.cecarmed.service.SalaEsperaService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DashboardController {

    @FXML private Label lblWelcome;
    @FXML private Label lblDate;
    @FXML private Label lblCitasHoy;
    @FXML private Label lblPacientesEnSala;
    @FXML private Label lblConsultasAtendidas;
    @FXML private Label lblTotalPacientes;

    private final CitaService citaService;
    private final SalaEsperaService salaEsperaService;
    private final PacienteService pacienteService;

    public DashboardController() {
        this(null, null, null);
    }

    public DashboardController(CitaService citaService, SalaEsperaService salaEsperaService, PacienteService pacienteService) {
        this.citaService = citaService;
        this.salaEsperaService = salaEsperaService;
        this.pacienteService = pacienteService;
    }

    @FXML
    public void initialize() {
        Usuario user = UserSession.getCurrentUser();
        if (user != null) {
            String prefix = switch (user.rol()) {
                case MEDICO -> "Dr(a). ";
                case ADMINISTRADOR -> "Admin ";
                case RECEPCIONISTA -> "Lic. ";
            };
            lblWelcome.setText("¡Hola, " + prefix + user.nombreCompleto() + "!");
        } else {
            lblWelcome.setText("¡Bienvenido(a) a CECAMed!");
        }

        LocalDate now = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.of("es", "ES"));
        String formattedDate = now.format(formatter);
        lblDate.setText(formattedDate.substring(0, 1).toUpperCase() + formattedDate.substring(1));

        cargarMetricasEnVivo();
    }

    private void cargarMetricasEnVivo() {
        if (citaService == null || salaEsperaService == null || pacienteService == null) {
            lblCitasHoy.setText("0");
            lblPacientesEnSala.setText("0");
            lblConsultasAtendidas.setText("0");
            lblTotalPacientes.setText("0");
            return;
        }

        Thread.ofVirtual().start(() -> {
            try {
                long citasHoy = citaService.contarCitasHoy();
                long enEspera = salaEsperaService.contarEnEsperaHoy();
                long totalPacientes = pacienteService.contarTotalPacientes();

                Platform.runLater(() -> {
                    lblCitasHoy.setText(String.valueOf(citasHoy));
                    lblPacientesEnSala.setText(String.valueOf(enEspera));
                    lblTotalPacientes.setText(String.valueOf(totalPacientes));
                });
            } catch (Exception e) {
                // Silencioso para no interrumpir la experiencia de usuario
            }
        });
    }
}
