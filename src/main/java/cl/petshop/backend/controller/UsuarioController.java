
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.UsuarioAdminRequest;
import cl.petshop.backend.dto.UsuarioResponse;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // Listar todos los usuarios
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::desde).toList();
    }

    // Buscar usuario por ID
    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return UsuarioResponse.desde(usuarioService.buscarPorId(id));
    }

    // Crear usuario desde administracion
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crear(
            @Valid @RequestBody UsuarioAdminRequest datos) {

        Usuario usuario = convertirAUsuario(datos);

        return UsuarioResponse.desde(usuarioService.crearPorAdministrador(
                usuario,
                datos.contrasena()
        ));
    }

    // Actualizar usuario
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAdminRequest datos) {

        Usuario usuario = convertirAUsuario(datos);

        return UsuarioResponse.desde(usuarioService.actualizarPorAdministrador(
                id,
                usuario,
                datos.contrasena()
        ));
    }

    // Eliminar usuario
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
    }

    // Convertir los datos recibidos a un objeto Usuario
    private Usuario convertirAUsuario(UsuarioAdminRequest datos) {

        Usuario usuario = new Usuario();

        usuario.setRun(datos.run());
        usuario.setNombre(datos.nombre());
        usuario.setApellidos(datos.apellidos());
        usuario.setCorreo(datos.correo());
        usuario.setRol(datos.rol());

        return usuario;
    }
}
