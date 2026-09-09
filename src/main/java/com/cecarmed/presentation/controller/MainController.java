package com.cecarmed.presentation.controller;

import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.session.UserSession;
import com.cecarmed.presentation.theme.ThemeManager;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.service.AuthService;
import com.cecarmed.service.CitaService;
import com.cecarmed.service.MedicoService;
import com.cecarmed.service.PacienteService;
import com.cecarmed.service.SalaEsperaService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private Label lblUserName;
    @FXML private Label lblUserRole;
    @FXML private Button btnThemeToggle;

    @FXML private Button btnNavDashboard;
    @FXML private Button btnNavPacientes;
    @FXML private Button btnNavMedicos;
    @FXML private Button btnNavCitas;
    @FXML private Button btnNavSalaEspera;
    @FXML private Button btnNavConfig;

    @FXML private StackPane contentArea;

    private final AuthService authService;
    private final PacienteService pacienteService;
    private final CitaService citaService;
    private final SalaEsperaService salaEsperaService;
    private final UsuarioRepository usuarioRepository;
    private final MedicoService medicoService;
    private Stage stage;

    public MainController(AuthService authService, PacienteService pacienteService,
                          CitaService citaService, SalaEsperaService salaEsperaService,
                          UsuarioRepository usuarioRepository, MedicoService medicoService) {
        this.authService = authService;
        this.pacienteService = pacienteService;
        this.citaService = citaService;
        this.salaEsperaService = salaEsperaService;
        this.usuarioRepository = usuarioRepository;
        this.medicoService = medicoService;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    public void initialize() {
        Usuario user = UserSession.getCurrentUser();
        if (user != null) {
            lblUserName.setText(user.nombreCompleto());
            lblUserRole.setText(user.rol().getDescripcion().toUpperCase());

            // Control de acceso basado en roles en el menú
            if (user.rol() != Rol.ADMINISTRADOR) {
                btnNavConfig.setVisible(false);
                btnNavConfig.setManaged(false);
            }
        }

        updateThemeButtonText();

        // Cargar vista por defecto (Dashboard)
        showDashboard();
    }

    @FXML
    public void showDashboard() {
        setActiveButton(btnNavDashboard);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/DashboardView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == DashboardController.class) {
                    return new DashboardController(citaService, salaEsperaService, pacienteService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            log.error("Error al cargar DashboardView", e);
        }
    }

    @FXML
    public void showPacientes() {
        setActiveButton(btnNavPacientes);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/PacientesView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == PacientesController.class) {
                    return new PacientesController(pacienteService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            log.error("Error al cargar PacientesView", e);
        }
    }

    @FXML
    public void showMedicos() {
        setActiveButton(btnNavMedicos);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/MedicosView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == MedicosController.class) {
                    return new MedicosController(medicoService);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            log.error("Error al cargar MedicosView", e);
        }
    }

    @FXML
    public void showCitas() {
        setActiveButton(btnNavCitas);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/CitasView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == CitasController.class) {
                    return new CitasController(citaService, pacienteService, usuarioRepository);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            log.error("Error al cargar CitasView", e);
        }
    }

    @FXML
    public void showSalaEspera() {
        setActiveButton(btnNavSalaEspera);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/SalaEsperaView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == SalaEsperaController.class) {
                    return new SalaEsperaController(salaEsperaService, citaService, pacienteService, usuarioRepository);
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            log.error("Error al cargar SalaEsperaView", e);
        }
    }

    @FXML
    public void showConfiguracion() {
        setActiveButton(btnNavConfig);
        showPlaceholderModule("Configuración del Sistema y Usuarios",
                "Gestión de personal médico, cuentas de usuario y auditoría de seguridad.");
    }

    @FXML
    public void handleToggleTheme() {
        ThemeManager.toggleTheme();
        updateThemeButtonText();
    }

    @FXML
    public void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cerrar Sesión");
        confirm.setHeaderText("¿Estás seguro de que deseas salir de CECAMed?");
        confirm.setContentText("Tu sesión actual será finalizada.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            authService.logout("localhost");
            navigateToLogin();
        }
    }

    private void navigateToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == LoginController.class) {
                    LoginController ctrl = new LoginController(authService, pacienteService, citaService, salaEsperaService, usuarioRepository, medicoService);
                    ctrl.setStage(stage);
                    return ctrl;
                }
                try {
                    return param.getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

            stage.setTitle("CECAMed - Inicio de Sesión");
            stage.setScene(scene);
            stage.setMinWidth(480);
            stage.setMinHeight(560);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (IOException e) {
            log.error("Error al volver a la vista de login", e);
        }
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            contentArea.getChildren().setAll(node);
        } catch (IOException e) {
            log.error("Error al cargar la vista: {}", fxmlPath, e);
        }
    }

    private void showPlaceholderModule(String title, String description) {
        StackPane placeholder = new StackPane();
        placeholder.setStyle("-fx-padding: 40px;");

        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(12);
        box.setAlignment(javafx.geometry.Pos.CENTER);
        box.getStyleClass().add("card");
        box.setMaxWidth(600);
        box.setStyle("-fx-padding: 30px;");

        Label lblTitle = new Label(title);
        lblTitle.getStyleClass().add("title-2");

        Label lblDesc = new Label(description);
        lblDesc.setWrapText(true);
        lblDesc.getStyleClass().add("text-muted");

        box.getChildren().addAll(lblTitle, lblDesc);
        placeholder.getChildren().add(box);

        contentArea.getChildren().setAll(placeholder);
    }

    private void setActiveButton(Button activeBtn) {
        List<Button> buttons = List.of(btnNavDashboard, btnNavPacientes, btnNavMedicos, btnNavCitas, btnNavSalaEspera, btnNavConfig);
        for (Button btn : buttons) {
            btn.getStyleClass().remove("nav-button-active");
        }
        if (!activeBtn.getStyleClass().contains("nav-button-active")) {
            activeBtn.getStyleClass().add("nav-button-active");
        }
    }

    private void updateThemeButtonText() {
        if (ThemeManager.isDarkMode()) {
            btnThemeToggle.setText("☀️ Modo Claro");
        } else {
            btnThemeToggle.setText("🌙 Modo Oscuro");
        }
    }
}
