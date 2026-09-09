package com.cecarmed.service;

import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;
import com.cecarmed.domain.repository.AuditoriaRepository;
import com.cecarmed.domain.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AuditoriaRepository auditoriaRepository;

    private MedicoService medicoService;

    @BeforeEach
    void setUp() {
        medicoService = new MedicoService(usuarioRepository, auditoriaRepository);
    }

    @Test
    @DisplayName("Debe registrar un médico correctamente con rol MEDICO y contraseña encriptada")
    void shouldRegisterMedicoSuccessfully() {
        when(usuarioRepository.existsByUsername("dr.house")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            return new Usuario(
                    10L,
                    u.nombreCompleto(),
                    u.username(),
                    u.passwordHash(),
                    u.rol(),
                    u.cedulaProfesional(),
                    u.especialidad(),
                    u.email(),
                    u.telefono(),
                    u.activo(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
            );
        });

        Usuario medico = medicoService.registrarMedico(
                "Dr. Gregory House",
                "dr.house",
                "Diagnostico1*",
                "MED-987654",
                "Medicina Interna",
                "house@cecarmed.com",
                "555-123-9999"
        );

        assertThat(medico).isNotNull();
        assertThat(medico.id()).isEqualTo(10L);
        assertThat(medico.nombreCompleto()).isEqualTo("Dr. Gregory House");
        assertThat(medico.username()).isEqualTo("dr.house");
        assertThat(medico.rol()).isEqualTo(Rol.MEDICO);
        assertThat(medico.especialidad()).isEqualTo("Medicina Interna");
        assertThat(medico.cedulaProfesional()).isEqualTo("MED-987654");
        assertThat(medico.activo()).isTrue();

        verify(usuarioRepository).save(any(Usuario.class));
        verify(auditoriaRepository).registrar(any());
    }

    @Test
    @DisplayName("Debe fallar al registrar si el nombre de usuario ya existe")
    void shouldFailWhenUsernameAlreadyExists() {
        when(usuarioRepository.existsByUsername("dr.house")).thenReturn(true);

        assertThatThrownBy(() -> medicoService.registrarMedico(
                "Dr. Gregory House",
                "dr.house",
                "Password123",
                "MED-987654",
                "Medicina Interna",
                "house@cecarmed.com",
                "555-123-9999"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ya existe un usuario registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe fallar si la cédula profesional o especialidad están vacías")
    void shouldFailWhenCedulaOrEspecialidadIsBlank() {
        assertThatThrownBy(() -> medicoService.registrarMedico(
                "Dr. Gregory House",
                "dr.house",
                "Password123",
                "",
                "Medicina Interna",
                "house@cecarmed.com",
                "555-123-9999"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cédula profesional es obligatoria");

        assertThatThrownBy(() -> medicoService.registrarMedico(
                "Dr. Gregory House",
                "dr.house",
                "Password123",
                "MED-12345",
                "  ",
                "house@cecarmed.com",
                "555-123-9999"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("especialidad médica es obligatoria");
    }

    @Test
    @DisplayName("Debe fallar si la contraseña tiene menos de 6 caracteres")
    void shouldFailWhenPasswordIsTooShort() {
        assertThatThrownBy(() -> medicoService.registrarMedico(
                "Dr. Gregory House",
                "dr.house",
                "12345",
                "MED-987654",
                "Medicina Interna",
                "house@cecarmed.com",
                "555-123-9999"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos 6 caracteres");
    }

    @Test
    @DisplayName("Debe listar todos los médicos y permitir búsqueda")
    void shouldListAndSearchMedicos() {
        Usuario m1 = Usuario.nuevo("Dr. A", "dra", "hash", Rol.MEDICO, "123", "Cardiología", null, null);
        when(usuarioRepository.findAllMedicos()).thenReturn(List.of(m1));
        when(usuarioRepository.searchMedicos("Cardio")).thenReturn(List.of(m1));

        List<Usuario> todos = medicoService.listarTodos();
        assertThat(todos).hasSize(1);

        List<Usuario> busqueda = medicoService.buscarMedicos("Cardio");
        assertThat(busqueda).hasSize(1);
    }

    @Test
    @DisplayName("Debe actualizar un médico existente y su contraseña opcional")
    void shouldUpdateMedicoSuccessfully() {
        Usuario existente = new Usuario(
                5L, "Dr. Antiguo", "dr.antiguo", "oldHash", Rol.MEDICO,
                "CED-1", "General", "old@cecarmed.com", "555-1111", true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(existente));

        medicoService.actualizarMedico(
                5L,
                "Dr. Actualizado",
                "NuevaClave123",
                "CED-2",
                "Neurología",
                "nuevo@cecarmed.com",
                "555-2222",
                true
        );

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).update(captor.capture());
        assertThat(captor.getValue().nombreCompleto()).isEqualTo("Dr. Actualizado");
        assertThat(captor.getValue().especialidad()).isEqualTo("Neurología");

        verify(usuarioRepository).updatePassword(eq(5L), anyString());
        verify(auditoriaRepository).registrar(any());
    }

    @Test
    @DisplayName("Debe activar/desactivar el estado del médico")
    void shouldToggleActivoMedico() {
        Usuario medico = new Usuario(
                7L, "Dra. Grey", "dr.grey", "hash", Rol.MEDICO,
                "CED-7", "Cirugía General", "grey@cecarmed.com", "555-7777", true,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(medico));

        medicoService.toggleActivoMedico(7L, false);

        verify(usuarioRepository).setActivo(7L, false);
        verify(auditoriaRepository).registrar(any());
    }
}
