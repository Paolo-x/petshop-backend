
package cl.petshop.backend.repository;

import cl.petshop.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Buscar usuario por correo
    Optional<Usuario> findByCorreoIgnoreCase(String correo);

    // Comprobar si existe un correo
    boolean existsByCorreoIgnoreCase(String correo);

    // Buscar usuario por RUN
    Optional<Usuario> findByRun(String run);

    // Comprobar si existe un RUN
    boolean existsByRun(String run);

    // Listar usuarios segun su rol
    List<Usuario> findByRol(Usuario.Rol rol);
}
