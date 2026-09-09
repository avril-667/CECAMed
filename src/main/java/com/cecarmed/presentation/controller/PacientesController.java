package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Paciente;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class PacientesController {

    private static final Logger log = LoggerFactory.getLogger(PacientesController.class);

    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private Button btnNuevoPaciente;
    @FXML private ProgressIndicator progressSearch;
    @FXML private Label lblTotalResultados;

    @FXML private TableView<Paciente> tblPacientes;
    @FXML private TableColumn<Paciente, String> colExpediente;
    @FXML private TableColumn<Paciente, String> colNombre;
    @FXML private TableColumn<Paciente, String> colEdad;
    @FXML private TableColumn<Paciente, String> colGenero;
    @FXML private TableColumn<Paciente, String> colTelefono;
    @FXML private TableColumn<Paciente, String> colAlergias;
    @FXML private TableColumn<Paciente, String> colEstado;
    @FXML private TableColumn<Paciente, Void> colAcciones;

    private final PacienteService pacienteService;
    private final ObservableList<Paciente> pacientesData = FXCollections.observableArrayList();

    public PacientesController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        tblPacientes.setItems(pacientesData);
        tblPacientes.setPlaceholder(new Label("No se encontraron pacientes registrados"));

        txtSearch.setOnAction(e -> handleSearch());
        cargarPacientes(null);
    }

    private void setupTableColumns() {
        colExpediente.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().expedienteNumero()));
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombreCompleto()));
        colEdad.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEdad() + " años"));
        colGenero.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().genero().getDescripcion()));
        colTelefono.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().telefono()));
        colAlergias.setCellValueFactory(data -> {
            String alergias = data.getValue().alergias();
            return new SimpleStringProperty(alergias != null && !alergias.isBlank() ? alergias : "Ninguna");
        });
        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().activo() ? "Activo" : "Inactivo")
        );

        // Columna de Acciones (Editar y Alternar Estado)
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEstado = new Button();
            private final HBox pane = new HBox(6, btnEditar, btnEstado);

            {
                btnEditar.getStyleClass().addAll("button-outlined", "small");
                btnEstado.getStyleClass().addAll("small");

                btnEditar.setOnAction(e -> {
                    Paciente p = getTableView().getItems().get(getIndex());
                    abrirModalPaciente(p);
                });

                btnEstado.setOnAction(e -> {
                    Paciente p = getTableView().getItems().get(getIndex());
                    toggleEstadoPaciente(p);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Paciente p = getTableView().getItems().get(getIndex());
                    if (p.activo()) {
                        btnEstado.setText("Desactivar");
                        btnEstado.getStyleClass().removeAll("success");
                        if (!btnEstado.getStyleClass().contains("danger")) {
                            btnEstado.getStyleClass().add("danger");
                        }
                    } else {
                        btnEstado.setText("Activar");
                        btnEstado.getStyleClass().removeAll("danger");
                        if (!btnEstado.getStyleClass().contains("success")) {
                            btnEstado.getStyleClass().add("success");
                        }
                    }
                    setGraphic(pane);
                }
            }
        });
    }

    @FXML
    public void handleSearch() {
        String query = txtSearch.getText();
        cargarPacientes(query);
    }

    private void cargarPacientes(String query) {
        progressSearch.setVisible(true);

        Thread.ofVirtual().start(() -> {
            try {
                List<Paciente> lista = pacienteService.buscarPacientes(query, 100, 1);
                long total = pacienteService.contarBusqueda(query);

                Platform.runLater(() -> {
                    pacientesData.setAll(lista);
                    lblTotalResultados.setText("Total: " + total + " paciente(s)");
                    progressSearch.setVisible(false);
                });
            } catch (Exception e) {
                log.error("Error al buscar pacientes", e);
                Platform.runLater(() -> progressSearch.setVisible(false));
            }
        });
    }

    @FXML
    public void handleNuevoPaciente() {
        abrirModalPaciente(null);
    }

    private void abrirModalPaciente(Paciente pacienteAEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PacienteDialogView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == PacienteDialogController.class) {
                    return new PacienteDialogController(pacienteService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            PacienteDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(pacienteAEditar == null ? "Nuevo Paciente" : "Editar Paciente");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            if (tblPacientes.getScene() != null && tblPacientes.getScene().getWindow() != null) {
                dialogStage.initOwner(tblPacientes.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            if (pacienteAEditar != null) {
                controller.setPacienteParaEditar(pacienteAEditar);
            }

            dialogStage.showAndWait();

            if (controller.isGuardadoExitoso()) {
                cargarPacientes(txtSearch.getText());
            }
        } catch (IOException e) {
            log.error("Error al abrir diálogo de paciente", e);
        }
    }

    private void toggleEstadoPaciente(Paciente p) {
        boolean nuevoEstado = !p.activo();
        String accion = nuevoEstado ? "activar" : "desactivar";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación");
        alert.setHeaderText("¿Deseas " + accion + " al paciente?");
        alert.setContentText(p.getNombreCompleto() + " (" + p.expedienteNumero() + ")");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Thread.ofVirtual().start(() -> {
                    try {
                        pacienteService.toggleActivoPaciente(p.id(), nuevoEstado);
                        Platform.runLater(() -> cargarPacientes(txtSearch.getText()));
                    } catch (Exception e) {
                        log.error("Error al cambiar estado del paciente", e);
                    }
                });
            }
        });
    }
}
