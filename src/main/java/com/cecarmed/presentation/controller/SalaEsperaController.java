package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.AtencionSala;
import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoSala;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.PacienteService;
import com.cecarmed.service.SalaEsperaService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SalaEsperaController {

    private static final Logger log = LoggerFactory.getLogger(SalaEsperaController.class);

    @FXML private Label lblTurnoLlamado;
    @FXML private Label lblPacienteLlamado;
    @FXML private Label lblMedicoLlamado;
    @FXML private Label lblHoraLlamado;
    @FXML private Label lblTotalEnEspera;
    @FXML private ProgressIndicator progressSala;

    @FXML private TableView<SalaRow> tblSalaEspera;
    @FXML private TableColumn<SalaRow, String> colTurno;
    @FXML private TableColumn<SalaRow, String> colPaciente;
    @FXML private TableColumn<SalaRow, String> colMedico;
    @FXML private TableColumn<SalaRow, String> colLlegada;
    @FXML private TableColumn<SalaRow, String> colTiempoEspera;
    @FXML private TableColumn<SalaRow, String> colEstado;
    @FXML private TableColumn<SalaRow, Void> colAcciones;

    private final SalaEsperaService salaEsperaService;
    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;

    private final ObservableList<SalaRow> salaData = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private ScheduledExecutorService timerService;

    public record SalaRow(AtencionSala atencion, String pacienteNombre, String medicoNombre) {}

    public SalaEsperaController(SalaEsperaService salaEsperaService,
                                CitaService citaService,
                                PacienteService pacienteService,
                                UsuarioRepository usuarioRepository) {
        this.salaEsperaService = salaEsperaService;
        this.citaService = citaService;
        this.pacienteService = pacienteService;
        this.usuarioRepository = usuarioRepository;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        tblSalaEspera.setItems(salaData);
        tblSalaEspera.setPlaceholder(new Label("No hay pacientes activos en la sala de espera"));

        cargarDatos();
        iniciarAutoRefresco();
    }

    private void setupTableColumns() {
        colTurno.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().atencion().turnoCodigo()));
        colPaciente.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().pacienteNombre()));
        colMedico.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().medicoNombre()));
        colLlegada.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().atencion().horaLlegada().format(TIME_FMT))
        );
        colTiempoEspera.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().atencion().getMinutosEspera() + " min")
        );
        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().atencion().estadoSala().getDescripcion())
        );

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnLlamar = new Button("📢 Llamar");
            private final Button btnAtender = new Button("🩺 Atender");
            private final Button btnFinalizar = new Button("✅ Finalizar");
            private final Button btnCancelar = new Button("❌");
            private final HBox pane = new HBox(4, btnLlamar, btnAtender, btnFinalizar, btnCancelar);

            {
                btnLlamar.getStyleClass().addAll("button-outlined", "accent", "small");
                btnAtender.getStyleClass().addAll("button-outlined", "success", "small");
                btnFinalizar.getStyleClass().addAll("button-outlined", "small");
                btnCancelar.getStyleClass().addAll("button-outlined", "danger", "small");

                btnLlamar.setOnAction(e -> {
                    SalaRow row = getTableView().getItems().get(getIndex());
                    llamar(row.atencion().id());
                });

                btnAtender.setOnAction(e -> {
                    SalaRow row = getTableView().getItems().get(getIndex());
                    atender(row.atencion().id());
                });

                btnFinalizar.setOnAction(e -> {
                    SalaRow row = getTableView().getItems().get(getIndex());
                    finalizar(row.atencion().id());
                });

                btnCancelar.setOnAction(e -> {
                    SalaRow row = getTableView().getItems().get(getIndex());
                    cancelar(row.atencion().id());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    SalaRow row = getTableView().getItems().get(getIndex());
                    EstadoSala estado = row.atencion().estadoSala();

                    btnLlamar.setVisible(estado == EstadoSala.ESPERANDO || estado == EstadoSala.LLAMADO);
                    btnLlamar.setManaged(estado == EstadoSala.ESPERANDO || estado == EstadoSala.LLAMADO);

                    btnAtender.setVisible(estado == EstadoSala.LLAMADO || estado == EstadoSala.ESPERANDO);
                    btnAtender.setManaged(estado == EstadoSala.LLAMADO || estado == EstadoSala.ESPERANDO);

                    btnFinalizar.setVisible(estado == EstadoSala.EN_CONSULTA);
                    btnFinalizar.setManaged(estado == EstadoSala.EN_CONSULTA);

                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    public void handleRefrescar() {
        cargarDatos();
    }

    @FXML
    public void handleCheckIn() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CheckInDialogView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == CheckInDialogController.class) {
                    return new CheckInDialogController(salaEsperaService, citaService, pacienteService, usuarioRepository);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            CheckInDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Check-In Sala de Espera");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            if (tblSalaEspera.getScene() != null && tblSalaEspera.getScene().getWindow() != null) {
                dialogStage.initOwner(tblSalaEspera.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isCheckInExitoso()) {
                cargarDatos();
            }
        } catch (IOException e) {
            log.error("Error al abrir diálogo de check-in", e);
        }
    }

    private void cargarDatos() {
        progressSala.setVisible(true);

        Thread.ofVirtual().start(() -> {
            try {
                List<AtencionSala> activos = salaEsperaService.obtenerActivosHoy();
                Optional<AtencionSala> ultimoLlamado = salaEsperaService.obtenerUltimoLlamado();
                long totalEspera = salaEsperaService.contarEnEsperaHoy();

                List<SalaRow> rows = new ArrayList<>();
                for (AtencionSala a : activos) {
                    Optional<Paciente> p = pacienteService.obtenerPorId(a.pacienteId());
                    String pacNombre = p.map(Paciente::getNombreCompleto).orElse("Paciente");

                    String medNombre = "Médico";
                    Optional<Cita> citaOpt = citaService.obtenerCitasPorFecha(a.horaLlegada().toLocalDate())
                            .stream().filter(c -> c.id().equals(a.citaId())).findFirst();
                    if (citaOpt.isPresent()) {
                        Optional<Usuario> medOpt = usuarioRepository.findById(citaOpt.get().medicoId());
                        medNombre = medOpt.map(u -> "Dr(a). " + u.nombreCompleto()).orElse("Médico");
                    }

                    rows.add(new SalaRow(a, pacNombre, medNombre));
                }

                Platform.runLater(() -> {
                    salaData.setAll(rows);
                    lblTotalEnEspera.setText("En espera: " + totalEspera + " paciente(s)");

                    // Actualizar Card de Último Llamado
                    if (ultimoLlamado.isPresent()) {
                        AtencionSala call = ultimoLlamado.get();
                        lblTurnoLlamado.setText(call.turnoCodigo());
                        Optional<Paciente> p = pacienteService.obtenerPorId(call.pacienteId());
                        lblPacienteLlamado.setText(p.map(Paciente::getNombreCompleto).orElse("Paciente"));
                        if (call.horaLlamado() != null) {
                            lblHoraLlamado.setText("Llamado a las: " + call.horaLlamado().format(TIME_FMT));
                        }
                    } else {
                        lblTurnoLlamado.setText("--");
                        lblPacienteLlamado.setText("Sin llamados activos");
                        lblHoraLlamado.setText("");
                    }

                    progressSala.setVisible(false);
                });
            } catch (Exception e) {
                log.error("Error al cargar sala de espera", e);
                Platform.runLater(() -> progressSala.setVisible(false));
            }
        });
    }

    private void llamar(Long atencionId) {
        Thread.ofVirtual().start(() -> {
            try {
                salaEsperaService.llamarPaciente(atencionId);
                Platform.runLater(this::cargarDatos);
            } catch (Exception e) {
                log.error("Error al llamar paciente", e);
            }
        });
    }

    private void atender(Long atencionId) {
        Thread.ofVirtual().start(() -> {
            try {
                salaEsperaService.iniciarConsulta(atencionId);
                Platform.runLater(this::cargarDatos);
            } catch (Exception e) {
                log.error("Error al iniciar consulta", e);
            }
        });
    }

    private void finalizar(Long atencionId) {
        Thread.ofVirtual().start(() -> {
            try {
                salaEsperaService.finalizarAtencion(atencionId);
                Platform.runLater(this::cargarDatos);
            } catch (Exception e) {
                log.error("Error al finalizar consulta", e);
            }
        });
    }

    private void cancelar(Long atencionId) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Cancelar Turno");
        alert.setHeaderText("¿Estás seguro de cancelar este turno de sala de espera?");
        alert.setContentText("El turno pasará a estado Cancelado / Abandono.");

        alert.showAndWait().ifPresent(res -> {
            if (res == ButtonType.OK) {
                Thread.ofVirtual().start(() -> {
                    try {
                        salaEsperaService.cancelarTurno(atencionId, "Cancelado desde monitor");
                        Platform.runLater(this::cargarDatos);
                    } catch (Exception e) {
                        log.error("Error al cancelar turno", e);
                    }
                });
            }
        });
    }

    private void iniciarAutoRefresco() {
        timerService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SalaEspera-AutoRefresh");
            t.setDaemon(true);
            return t;
        });
        timerService.scheduleAtFixedRate(() -> {
            Platform.runLater(this::cargarDatos);
        }, 15, 15, TimeUnit.SECONDS);
    }
}
