package com.cecarmed.presentation.controller;

import com.cecarmed.domain.exception.AuthenticationException;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.session.UserSession;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.service.AuthService;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.PacienteService;
import com.cecarmed.service.SalaEsperaService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Button btnLogin;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblError;
    @FXML private VBox errorContainer;

    private final AuthService authService;
    private final PacienteService pacienteService;
    private final CitaService citaService;
    private final SalaEsperaService salaEsperaService;
    private final UsuarioRepository usuarioRepository;
    private final com.cecarmed.service.MedicoService medicoService;
    private Stage stage;

    public LoginController(AuthService authService, PacienteService pacienteService,
                           CitaService citaService, SalaEsperaService salaEsperaService,
                           UsuarioRepository usuarioRepository,
                           com.cecarmed.service.MedicoService medicoService) {
        this.authService = authService;
        this.pacienteService = pacienteService;
        this.citaService = citaService;
        this.salaEsperaService = salaEsperaService;
        this.usuarioRepository = usuarioRepository;
        this.medicoService = medicoService;
    }

    @FXML
    public void initialize() {
        hideError();
        progressIndicator.setVisible(false);

        // Enter key activates login
        txtUsername.setOnAction(e -> txtPassword.requestFocus());
        txtPassword.setOnAction(e -> handleLogin());
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            showError("Por favor, ingresa tu usuario y contraseña.");
            return;
        }

        setLoadingState(true);
        hideError();

        // Operación I/O segura en Virtual Thread para no bloquear el UI Thread de JavaFX
        Thread.ofVirtual().start(() -> {
            try {
                Usuario usuario = authService.login(username, password, "localhost");
                Platform.runLater(() -> {
                    setLoadingState(false);
                    onLoginSuccess(usuario);
                });
            } catch (AuthenticationException e) {
                Platform.runLater(() -> {
                    setLoadingState(false);
                    showError(e.getMessage());
                });
            } catch (Exception e) {
                log.error("Error inesperado en proceso de login", e);
                Platform.runLater(() -> {
                    setLoadingState(false);
                    showError("Error de conexión con el servidor. Intente nuevamente.");
                });
            }
        });
    }

    private void onLoginSuccess(Usuario usuario) {
        log.info("Acceso concedido a {} [{}]", usuario.nombreCompleto(), usuario.rol());
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/views/MainView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == MainController.class) {
                    MainController ctrl = new MainController(authService, pacienteService, citaService, salaEsperaService, usuarioRepository, medicoService);
                    ctrl.setStage(stage);
                    return ctrl;
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            stage.setTitle("CECAMed - Sistema de Control y Gestión Médica");
            stage.setScene(scene);
            stage.setMinWidth(1000);
            stage.setMinHeight(680);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (java.io.IOException e) {
            log.error("Error al cargar MainView", e);
            showError("Error al cargar el panel principal.");
        }
    }

    private void setLoadingState(boolean loading) {
        btnLogin.setDisable(loading);
        txtUsername.setDisable(loading);
        txtPassword.setDisable(loading);
        progressIndicator.setVisible(loading);
    }

    private void showError(String message) {
        lblError.setText(message);
        errorContainer.setVisible(true);
        errorContainer.setManaged(true);
    }

    private void hideError() {
        errorContainer.setVisible(false);
        errorContainer.setManaged(false);
        lblError.setText("");
    }
}
