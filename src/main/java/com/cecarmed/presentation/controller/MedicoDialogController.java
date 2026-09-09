package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Usuario;
import com.cecarmed.service.MedicoService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MedicoDialogController {

    private static final Logger log = LoggerFactory.getLogger(MedicoDialogController.class);

    private static final List<String> ESPECIALIDADES_SUGERIDAS = List.of(
            "Medicina General",
            "Cardiología",
            "Pediatría",
            "Ginecología y Obstetricia",
            "Traumatología y Ortopedia",
            "Dermatología",
            "Medicina Interna",
            "Neurología",
            "Oftalmología",
            "Otorrinolaringología",
            "Psiquiatría",
            "Cirugía General",
            "Anestesiología",
            "Radiología",
            "Urología",
            "Endocrinología",
            "Nutriología Clínica",
            "Urgencias Médicas"
    );

    @FXML private Label lblDialogTitle;
    @FXML private TextField txtNombreCompleto;
    @FXML private TextField txtCedula;
    @FXML private ComboBox<String> cbEspecialidad;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtUsername;
    @FXML private Label lblPassword;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private final MedicoService medicoService;
    private Stage dialogStage;
    private Usuario medicoActual; // null si es nuevo
    private boolean guardadoExitoso = false;

    public MedicoDialogController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    public void initialize() {
        cbEspecialidad.setItems(FXCollections.observableArrayList(ESPECIALIDADES_SUGERIDAS));
        cbEspecialidad.getSelectionModel().selectFirst();
        hideError();
    }

    public void setMedicoParaEditar(Usuario medico) {
        this.medicoActual = medico;
        lblDialogTitle.setText("Editar Médico: " + medico.nombreCompleto());

        txtNombreCompleto.setText(medico.nombreCompleto());
        txtCedula.setText(medico.cedulaProfesional() != null ? medico.cedulaProfesional() : "");

        if (medico.especialidad() != null) {
            cbEspecialidad.setValue(medico.especialidad());
        }

        txtTelefono.setText(medico.telefono() != null ? medico.telefono() : "");
        txtEmail.setText(medico.email() != null ? medico.email() : "");

        txtUsername.setText(medico.username());
        txtUsername.setDisable(true); // El username no se modifica tras la creación

        lblPassword.setText("Nueva Contraseña (opcional)");
        txtPassword.setPromptText("Dejar en blanco para mantener la actual");
    }

    @FXML
    public void handleGuardar() {
        hideError();

        String nombre = txtNombreCompleto.getText();
        String cedula = txtCedula.getText();
        String especialidad = cbEspecialidad.getEditor() != null && !cbEspecialidad.getEditor().getText().isBlank()
                ? cbEspecialidad.getEditor().getText().trim()
                : cbEspecialidad.getValue();
        String telefono = txtTelefono.getText();
        String email = txtEmail.getText();
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        btnGuardar.setDisable(true);

        Thread.ofVirtual().start(() -> {
            try {
                if (medicoActual == null) {
                    // Creación de nuevo médico
                    medicoService.registrarMedico(
                            nombre,
                            username,
                            password,
                            cedula,
                            especialidad,
                            email,
                            telefono
                    );
                } else {
                    // Actualización de médico existente
                    medicoService.actualizarMedico(
                            medicoActual.id(),
                            nombre,
                            password,
                            cedula,
                            especialidad,
                            email,
                            telefono,
                            medicoActual.activo()
                    );
                }

                Platform.runLater(() -> {
                    guardadoExitoso = true;
                    dialogStage.close();
                });
            } catch (IllegalArgumentException e) {
                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                log.error("Error al guardar médico", e);
                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    showError("Error inesperado al guardar médico. Verifique los datos o la conexión.");
                });
            }
        });
    }

    @FXML
    public void handleCancelar() {
        dialogStage.close();
    }

    public boolean isGuardadoExitoso() {
        return guardadoExitoso;
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
