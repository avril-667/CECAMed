package com.cecarmed;

import atlantafx.base.theme.PrimerLight;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.UsuarioRepository;
import com.cecarmed.infrastructure.db.DataSourceProvider;
import com.cecarmed.infrastructure.persistence.JdbcAuditoriaRepository;
import com.cecarmed.infrastructure.persistence.JdbcUsuarioRepository;
import com.cecarmed.presentation.controller.LoginController;
import com.cecarmed.service.AuthService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * Punto de entrada principal de la aplicación CECAMed en JavaFX 21+.
 */
public class CECAMedApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(CECAMedApp.class);

    private AuthService authService;
    private com.cecarmed.service.PacienteService pacienteService;
    private com.cecarmed.service.CitaService citaService;
    private com.cecarmed.service.SalaEsperaService salaEsperaService;
    private com.cecarmed.service.MedicoService medicoService;
    private UsuarioRepository usuarioRepository;

    @Override
    public void init() throws Exception {
        log.info("Iniciando infraestructura de CECAMed...");

        // Tema AtlantaFX por defecto
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // Inicialización de persistencia y servicios
        try {
            DataSource dataSource = DataSourceProvider.getDataSource();
            this.usuarioRepository = new JdbcUsuarioRepository(dataSource);
            AuditoriaRepository auditoriaRepository = new JdbcAuditoriaRepository(dataSource);
            this.authService = new AuthService(usuarioRepository, auditoriaRepository);

            com.cecarmed.domain.repository.PacienteRepository pacienteRepository =
                    new com.cecarmed.infrastructure.persistence.JdbcPacienteRepository(dataSource);
            com.cecarmed.domain.repository.ExpedienteClinicoRepository expedienteClinicoRepository =
                    new com.cecarmed.infrastructure.persistence.JdbcExpedienteClinicoRepository(dataSource);
            this.pacienteService = new com.cecarmed.service.PacienteService(pacienteRepository, expedienteClinicoRepository, auditoriaRepository);

            com.cecarmed.domain.repository.HorarioAtencionRepository horarioRepository =
                    new com.cecarmed.infrastructure.persistence.JdbcHorarioAtencionRepository(dataSource);
            com.cecarmed.domain.repository.BloqueoAgendaRepository bloqueoRepository =
                    new com.cecarmed.infrastructure.persistence.JdbcBloqueoAgendaRepository(dataSource);
            com.cecarmed.domain.repository.CitaRepository citaRepository =
                    new com.cecarmed.infrastructure.persistence.JdbcCitaRepository(dataSource);
            com.cecarmed.infrastructure.calendar.GoogleCalendarService googleCalendarService =
                    new com.cecarmed.infrastructure.calendar.GoogleCalendarService();

            this.citaService = new com.cecarmed.service.CitaService(citaRepository, horarioRepository,
                    bloqueoRepository, googleCalendarService, auditoriaRepository);

            com.cecarmed.domain.repository.AtencionSalaRepository atencionRepository =
                    new com.cecarmed.infrastructure.persistence.JdbcAtencionSalaRepository(dataSource);
            this.salaEsperaService = new com.cecarmed.service.SalaEsperaService(atencionRepository, citaRepository, auditoriaRepository);

            this.medicoService = new com.cecarmed.service.MedicoService(usuarioRepository, auditoriaRepository);

            // Sembrar administrador inicial si no existe ninguno
            authService.seedDefaultAdminIfEmpty();
        } catch (Exception e) {
            log.error("Error al inicializar la base de datos o servicios: {}", e.getMessage(), e);
            // La aplicación podrá abrir pero notificará error al intentar autenticar
        }
    }

    @Override
    public void start(Stage primaryStage) {
        log.info("Cargando vista de autenticación...");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/LoginView.fxml"));
            loader.setControllerFactory(param -> {
                if (param == LoginController.class) {
                    LoginController controller = new LoginController(authService, pacienteService, citaService, salaEsperaService, usuarioRepository, medicoService);
                    controller.setStage(primaryStage);
                    return controller;
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

            primaryStage.setTitle("CECAMed - Inicio de Sesión");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(480);
            primaryStage.setMinHeight(560);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            log.error("Fallo al cargar la vista de login: {}", e.getMessage(), e);
            throw new RuntimeException("No fue posible cargar LoginView.fxml", e);
        }
    }

    @Override
    public void stop() {
        log.info("Cerrando aplicación CECAMed...");
        DataSourceProvider.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
