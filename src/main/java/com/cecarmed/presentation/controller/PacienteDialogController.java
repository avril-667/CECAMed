package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Genero;
import com.cecarmed.domain.model.Paciente;
import com.cecarmed.service.PacienteService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

public class PacienteDialogController {

    private static final Logger log = LoggerFactory.getLogger(PacienteDialogController.class);

    @FXML private Label lblDialogTitle;
    @FXML private TextField txtNombre;
    @FXML private TextField txtPrimerApellido;
    @FXML private TextField txtSegundoApellido;
    @FXML private DatePicker dpFechaNacimiento;
    @FXML private ComboBox<Genero> cbGenero;
    @FXML private TextField txtCurp;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextArea txtDireccion;
    @FXML private TextArea txtAlergias;
    @FXML private TextArea txtAntecedentesPatologicos;
    @FXML private TextArea txtAntecedentesNoPatologicos;
    @FXML private TextArea txtAntecedentesHeredo;
    @FXML private Label lblError;
    @FXML private Button btnGuardar;

    private final PacienteService pacienteService;
    private Stage dialogStage;
    private Paciente pacienteActual; // null si es nuevo
    private boolean guardadoExitoso = false;

    public PacienteDialogController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    public void initialize() {
        cbGenero.getItems().setAll(Genero.values());
        cbGenero.getSelectionModel().select(Genero.MASCULINO);
        hideError();
    }

    public void setPacienteParaEditar(Paciente paciente) {
        this.pacienteActual = paciente;
        lblDialogTitle.setText("Editar Paciente (" + paciente.expedienteNumero() + ")");

        txtNombre.setText(paciente.nombre());
        txtPrimerApellido.setText(paciente.primerApellido());
        txtSegundoApellido.setText(paciente.segundoApellido() != null ? paciente.segundoApellido() : "");
        dpFechaNacimiento.setValue(paciente.fechaNacimiento());
        cbGenero.setValue(paciente.genero());
        txtCurp.setText(paciente.curp() != null ? paciente.curp() : "");
        txtTelefono.setText(paciente.telefono());
        txtEmail.setText(paciente.email() != null ? paciente.email() : "");
        txtDireccion.setText(paciente.direccion() != null ? paciente.direccion() : "");
        txtAlergias.setText(paciente.alergias() != null ? paciente.alergias() : "");
        txtAntecedentesPatologicos.setText(paciente.antecedentesPatologicos() != null ? paciente.antecedentesPatologicos() : "");
        txtAntecedentesNoPatologicos.setText(paciente.antecedentesNoPatologicos() != null ? paciente.antecedentesNoPatologicos() : "");
        txtAntecedentesHeredo.setText(paciente.antecedentesHeredofamiliares() != null ? paciente.antecedentesHeredofamiliares() : "");
    }

    @FXML
    public void handleGuardar() {
        hideError();

        String nombre = txtNombre.getText();
        String primerApellido = txtPrimerApellido.getText();
        String segundoApellido = txtSegundoApellido.getText();
        LocalDate fechaNacimiento = dpFechaNacimiento.getValue();
        Genero genero = cbGenero.getValue();
        String curp = txtCurp.getText();
        String telefono = txtTelefono.getText();
        String email = txtEmail.getText();
        String direccion = txtDireccion.getText();
        String alergias = txtAlergias.getText();
        String patologicos = txtAntecedentesPatologicos.getText();
        String noPatologicos = txtAntecedentesNoPatologicos.getText();
        String heredo = txtAntecedentesHeredo.getText();

        btnGuardar.setDisable(true);

        Thread.ofVirtual().start(() -> {
            try {
                if (pacienteActual == null) {
                    // Creación
                    Paciente nuevo = Paciente.nuevo(
                            null,
                            nombre,
                            primerApellido,
                            segundoApellido,
                            fechaNacimiento,
                            genero,
                            curp,
                            telefono,
                            email,
                            direccion,
                            alergias,
                            patologicos,
                            noPatologicos,
                            heredo
                    );
                    pacienteService.registrarPaciente(nuevo);
                } else {
                    // Edición
                    Paciente actualizado = new Paciente(
                            pacienteActual.id(),
                            pacienteActual.expedienteNumero(),
                            nombre != null ? nombre.trim() : "",
                            primerApellido != null ? primerApellido.trim() : "",
                            segundoApellido != null && !segundoApellido.isBlank() ? segundoApellido.trim() : null,
                            fechaNacimiento,
                            genero,
                            curp != null && !curp.isBlank() ? curp.trim().toUpperCase() : null,
                            telefono != null ? telefono.trim() : "",
                            email != null && !email.isBlank() ? email.trim().toLowerCase() : null,
                            direccion,
                            alergias,
                            patologicos,
                            noPatologicos,
                            heredo,
                            pacienteActual.activo(),
                            pacienteActual.fechaRegistro(),
                            pacienteActual.fechaActualizacion()
                    );
                    pacienteService.actualizarPaciente(actualizado);
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
                log.error("Error al guardar paciente", e);
                Platform.runLater(() -> {
                    btnGuardar.setDisable(false);
                    showError("Error inesperado al guardar paciente. Revise la conexión.");
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
