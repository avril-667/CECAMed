package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Usuario;
import com.cecarmed.service.MedicoService;
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

public class MedicosController {

    private static final Logger log = LoggerFactory.getLogger(MedicosController.class);

    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private Button btnNuevoMedico;
    @FXML private ProgressIndicator progressSearch;
    @FXML private Label lblTotalResultados;

    @FXML private TableView<Usuario> tblMedicos;
    @FXML private TableColumn<Usuario, String> colCedula;
    @FXML private TableColumn<Usuario, String> colNombre;
    @FXML private TableColumn<Usuario, String> colEspecialidad;
    @FXML private TableColumn<Usuario, String> colUsername;
    @FXML private TableColumn<Usuario, String> colTelefono;
    @FXML private TableColumn<Usuario, String> colEmail;
    @FXML private TableColumn<Usuario, String> colEstado;
    @FXML private TableColumn<Usuario, Void> colAcciones;

    private final MedicoService medicoService;
    private final ObservableList<Usuario> medicosData = FXCollections.observableArrayList();

    public MedicosController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        tblMedicos.setItems(medicosData);
        tblMedicos.setPlaceholder(new Label("No se encontraron médicos registrados"));

        txtSearch.setOnAction(e -> handleSearch());
        cargarMedicos(null);
    }

    private void setupTableColumns() {
        colCedula.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().cedulaProfesional() != null ? data.getValue().cedulaProfesional() : "S/C"));

        colNombre.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().nombreCompleto()));

        colEspecialidad.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().especialidad() != null ? data.getValue().especialidad() : "Medicina General"));

        colUsername.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().username()));

        colTelefono.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().telefono() != null ? data.getValue().telefono() : "-"));

        colEmail.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().email() != null ? data.getValue().email() : "-"));

        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().activo() ? "Activo" : "Inactivo"));

        // Columna de Acciones (Editar y Alternar Estado)
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnEstado = new Button();
            private final HBox pane = new HBox(6, btnEditar, btnEstado);

            {
                btnEditar.getStyleClass().addAll("button-outlined", "small");
                btnEstado.getStyleClass().addAll("small");

                btnEditar.setOnAction(e -> {
                    Usuario m = getTableView().getItems().get(getIndex());
                    abrirModalMedico(m);
                });

                btnEstado.setOnAction(e -> {
                    Usuario m = getTableView().getItems().get(getIndex());
                    toggleEstadoMedico(m);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Usuario m = getTableView().getItems().get(getIndex());
                    if (m.activo()) {
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
        cargarMedicos(query);
    }

    private void cargarMedicos(String query) {
        progressSearch.setVisible(true);

        Thread.ofVirtual().start(() -> {
            try {
                List<Usuario> lista = medicoService.buscarMedicos(query);

                Platform.runLater(() -> {
                    medicosData.setAll(lista);
                    lblTotalResultados.setText("Total: " + lista.size() + " médico(s)");
                    progressSearch.setVisible(false);
                });
            } catch (Exception e) {
                log.error("Error al buscar médicos", e);
                Platform.runLater(() -> progressSearch.setVisible(false));
            }
        });
    }

    @FXML
    public void handleNuevoMedico() {
        abrirModalMedico(null);
    }

    private void abrirModalMedico(Usuario medicoAEditar) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MedicoDialogView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == MedicoDialogController.class) {
                    return new MedicoDialogController(medicoService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            MedicoDialogController controller = loader.getController();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(medicoAEditar == null ? "Registrar Nuevo Médico" : "Editar Médico");
            dialogStage.initModality(Modality.APPLICATION_MODAL);

            if (tblMedicos.getScene() != null && tblMedicos.getScene().getWindow() != null) {
                dialogStage.initOwner(tblMedicos.getScene().getWindow());
            }

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());
            dialogStage.setScene(scene);
            dialogStage.setResizable(false);

            controller.setDialogStage(dialogStage);
            if (medicoAEditar != null) {
                controller.setMedicoParaEditar(medicoAEditar);
            }

            dialogStage.showAndWait();

            if (controller.isGuardadoExitoso()) {
                cargarMedicos(txtSearch.getText());
            }
        } catch (IOException e) {
            log.error("Error al abrir diálogo de médico", e);
        }
    }

    private void toggleEstadoMedico(Usuario m) {
        boolean nuevoEstado = !m.activo();
        String accion = nuevoEstado ? "activar" : "desactivar";

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmación de Estado");
        alert.setHeaderText("¿Deseas " + accion + " al médico?");
        alert.setContentText("Dr(a). " + m.nombreCompleto() + " (" + m.username() + ")");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                Thread.ofVirtual().start(() -> {
                    try {
                        medicoService.toggleActivoMedico(m.id(), nuevoEstado);
                        Platform.runLater(() -> cargarMedicos(txtSearch.getText()));
                    } catch (Exception e) {
                        log.error("Error al cambiar estado del médico", e);
                    }
                });
            }
        });
    }
}
