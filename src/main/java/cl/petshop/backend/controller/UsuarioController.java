
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.UsuarioAdminRequest;
import cl.petshop.backend.dto.UsuarioResponse;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Gestión de cuentas. Solo para administración.")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Listar usuarios", responses = {
            @ApiResponse(responseCode = "200", description = "Listado sin contraseñas"),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content)
    })
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar().stream().map(UsuarioResponse::desde).toList();
    }

    @Operation(summary = "Ver un usuario", responses = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return UsuarioResponse.desde(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Crear un usuario", description = "Permite elegir el rol de la cuenta.", responses = {
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos de la cuenta incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "409", description = "RUN o correo ya registrado", content = @Content)
    })
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

    @Operation(summary = "Actualizar un usuario", description = "Si no se envía contraseña, conserva la anterior.", responses = {
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "400", description = "Identificador o datos incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "RUN o correo ya registrado", content = @Content)
    })
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

    @Operation(summary = "Eliminar un usuario", responses = {
            @ApiResponse(responseCode = "204", description = "Usuario eliminado", content = @Content),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "El usuario tiene pedidos asociados", content = @Content)
    })
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
