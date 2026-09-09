package com.cecarmed.domain.repository;

import com.cecarmed.domain.model.Rol;
import com.cecarmed.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

/**
 * Puerto del repositorio de usuarios (Clean Architecture).
 */
public interface UsuarioRepository {

    Optional<Usuario> findById(Long id);

    Optional<Usuario> findByUsername(String username);

    List<Usuario> findAll();

    List<Usuario> findByRol(Rol rol);

    List<Usuario> findAllMedicos();

    List<Usuario> searchMedicos(String query);

    Usuario save(Usuario usuario);

    void update(Usuario usuario);

    void updatePassword(Long usuarioId, String newPasswordHash);

    void setActivo(Long usuarioId, boolean activo);

    boolean existsByUsername(String username);

    long count();
}
