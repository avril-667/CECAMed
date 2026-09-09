package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Cita;
import com.cecarmed.domain.model.EstadoCita;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.PacienteService;
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
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class CitasController {

    private static final Logger log = LoggerFactory.getLogger(CitasController.class);

    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Usuario> cbFiltroMedico;
    @FXML private Button btnNuevaCita;
    @FXML private ProgressIndicator progressCitas;
    @FXML private Label lblTotalCitas;

    @FXML private TableView<CitaRow> tblCitas;
    @FXML private TableColumn<CitaRow, String> colHorario;
    @FXML private TableColumn<CitaRow, String> colPaciente;
    @FXML private TableColumn<CitaRow, String> colMedico;
    @FXML private TableColumn<CitaRow, String> colMotivo;
    @FXML private TableColumn<CitaRow, String> colEstado;
    @FXML private TableColumn<CitaRow, Void> colAcciones;

    private final CitaService citaService;
    private final PacienteService pacienteService;
    private final UsuarioRepository usuarioRepository;

    private final ObservableList<CitaRow> citasData = FXCollections.observableArrayList();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public record CitaRow(Cita cita, String pacienteNombre, String medicoNombre) {}

    public CitasController(CitaService citaService, PacienteService pacienteService, UsuarioRepository usuarioRepository) {
        this.citaService = citaService;
        this.pacienteService = pacienteService;
        this.usuarioRepository = usuarioRepository;
    }

    @FXML
    public void initialize() {
        dpFecha.setValue(LocalDate.now());
        setupTableColumns();
        tblCitas.setItems(citasData);
        tblCitas.setPlaceholder(new Label("No hay citas programadas para esta fecha"));

        dpFecha.valueProperty().addListener((obs, oldVal, newVal) -> cargarCitas());
        cbFiltroMedico.valueProperty().addListener((obs, oldVal, newVal) -> cargarCitas());

        cargarMedicos();
        cargarCitas();
    }

    private void setupTableColumns() {
        colHorario.setCellValueFactory(data -> {
            Cita c = data.getValue().cita();
            String start = c.fechaHoraInicio().format(TIME_FMT);
            String end = c.fechaHoraFin().format(TIME_FMT);
            return new SimpleStringProperty(start + " - " + end);
        });

        colPaciente.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().pacienteNombre()));
        colMedico.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().medicoNombre()));
        colMotivo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().cita().motivoConsulta()));
        colEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().cita().estado().getDescripcion()));

        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnConfirmar = new Button("Confirmar");
            private final Button btnCancelar = new Button("Cancelar");
            private final HBox pane = new HBox(6, btnConfirmar, btnCancelar);

            {
                btnConfirmar.getStyleClass().addAll("button-outlined", "success", "small");
                btnCancelar.getStyleClass().addAll("button-outlined", "danger", "small");

                btnConfirmar.setOnAction(e -> {
                    CitaRow row = getTableView().getItems().get(getIndex());
                    actualizarEstado(row.cita().id(), EstadoCita.CONFIRMADA);
                });

                btnCancelar.setOnAction(e -> {
                    CitaRow row = getTableView().getItems().get(getIndex());
                    confirmarCancelacion(row.cita().id());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CitaRow row = getTableView().getItems().get(getIndex());
                    btnConfirmar.setVisible(row.cita().estado() == EstadoCita.PROGRAMADA);
                    btnConfirmar.setManaged(row.cita().estado() == EstadoCita.PROGRAMADA);
                    btnCancelar.setVisible(row.cita().estado().esActiva());
                    btnCancelar.setManaged(row.cita().estado().esActiva());
                    setGraphic(pane);
                }
            }
        });
    }

    private void cargarMedicos() {
        Thread.ofVirtual().start(() -> {
            try {
                List<Usuario> medicos = usuarioRepository.findByRol(Rol.MEDICO);
                Platform.runLater(() -> {
                    cbFiltroMedico.getItems().setAll(medicos);
                    cbFiltroMedico.setConverter(new StringConverter<>() {
                        @Override
                        public String toString(Usuario u) {
                            return u == null ? "Todos los médicos" : "Dr(a). " + u.nombreCompleto();
                        }
                        @Override
                        public Usuario fromString(String string) { return null; }
                    });
                });
            } catch (Exception e) {
                log.error("Error al cargar médicos", e);
            }
        });
    }

    @FXML
    public void handleRefrescar() {
        cargarCitas();
    }

    private void cargarCitas() {
        LocalDate fecha = dpFecha.getValue();
        if (fecha == null) return;

        progressCitas.setVisible(true);
        Usuario medicoSeleccionado = cbFiltroMedico.getValue();

        Thread.ofVirtual().start(() -> {
            try {
                List<Cita> citas;
                if (medicoSeleccionado == null) {
                    citas = citaService.obtenerCitasPorFecha(fecha);
                } else {
                    citas = citaService.obtenerCitasPorFechaYMedico(fecha, medicoSeleccionado.id());
                }

                // Cargar nombres de pacientes y médicos
                List<CitaRow> rows = new ArrayList<>();
                for (Cita c : citas) {
                    Optional<Paciente> p = pacienteService.obtenerPorId(c.pacienteId());
                    Optional<Usuario> m = usuarioRepository.findById(c.medicoId());

                    String pacNombre = p.map(Paciente::getNombreCompleto).orElse("Paciente Desconocido");
                    String medNombre = m.map(u -> "Dr(a). " + u.nombreCompleto()).orElse("Médico Desconocido");

                    rows.add(new CitaRow(c, pacNombre, medNombre));
                }

                Platform.runLater(() -> {
                    citasData.setAll(rows);
                    lblTotalCitas.setText("Total: " + rows.size() + " cita(s)");
                    progressCitas.setVisible(false);
                });
            } catch (Exception e) {
                log.error("Error al cargar citas para la fecha {}", fecha, e);
                Platform.runLater(() -> progressCitas.setVisible(false));
            }
        });
    }

    @FXML
    public void handleNuevaCita() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CitaDialogView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == CitaDialogController.class) {
                    return new CitaDialogController(citaService, pacienteService, usuarioRepository);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            CitaDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Agendar Nueva Cita Médica");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            if (tblCitas.getScene() != null && tblCitas.getScene().getWindow() != null) {
                dialogStage.initOwner(tblCitas.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            dialogStage.showAndWait();

            if (controller.isAgendadaExitosa()) {
                cargarCitas();
            }
        } catch (IOException e) {
            log.error("Error al abrir diálogo de citas", e);
        }
    }

    private void actualizarEstado(Long citaId, EstadoCita nuevoEstado) {
        Thread.ofVirtual().start(() -> {
            try {
                citaService.cambiarEstado(citaId, nuevoEstado);
                Platform.runLater(this::cargarCitas);
            } catch (Exception e) {
                log.error("Error al actualizar estado de la cita", e);
            }
        });
    }

    private void confirmarCancelacion(Long citaId) {
        TextInputDialog dialog = new TextInputDialog("Cancelada a petición del paciente");
        dialog.setTitle("Cancelar Cita");
        dialog.setHeaderText("¿Confirmas la cancelación de la cita seleccionada?");
        dialog.setContentText("Motivo de cancelación:");

        dialog.showAndWait().ifPresent(motivo -> {
            Thread.ofVirtual().start(() -> {
                try {
                    citaService.cancelarCita(citaId, motivo);
                    Platform.runLater(this::cargarCitas);
                } catch (Exception e) {
                    log.error("Error al cancelar la cita", e);
                }
            });
        });
    }
}
