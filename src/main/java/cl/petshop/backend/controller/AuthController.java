
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.AuthResponse;
import cl.petshop.backend.dto.LoginRequest;
import cl.petshop.backend.dto.RegistroRequest;
import cl.petshop.backend.dto.UsuarioResponse;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.security.JwtService;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Sesión", description = "Registro, inicio y renovación de sesión.")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(
            UsuarioService usuarioService,
            JwtService jwtService) {

        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Registrar un cliente", description = "El registro público siempre crea el rol cliente.", responses = {
            @ApiResponse(responseCode = "201", description = "Cliente registrado"),
            @ApiResponse(responseCode = "400", description = "Datos del registro incorrectos", content = @Content),
            @ApiResponse(responseCode = "409", description = "RUN o correo ya registrado", content = @Content)
    })
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(
            @Valid @RequestBody RegistroRequest datos) {

        Usuario usuario = new Usuario();

        usuario.setRun(datos.run());
        usuario.setNombre(datos.nombre());
        usuario.setApellidos(datos.apellidos());
        usuario.setCorreo(datos.correo());

        return UsuarioResponse.desde(usuarioService.registrarCliente(
                usuario,
                datos.contrasena()
        ));
    }

    @Operation(summary = "Iniciar sesión", description = "Devuelve el JWT y los datos básicos del usuario.", responses = {
            @ApiResponse(responseCode = "200", description = "Sesión iniciada"),
            @ApiResponse(responseCode = "400", description = "Faltan datos o tienen un formato incorrecto", content = @Content),
            @ApiResponse(responseCode = "401", description = "Correo o contraseña incorrectos", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest datos) {

        Usuario usuario;

        try {
            usuario = usuarioService.buscarPorCorreo(
                    datos.correo()
            );
        } catch (ResponseStatusException e) {

            if (e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Correo o contrasena incorrectos"
                );
            }

            throw e;
        }

        if (!usuarioService.comprobarContrasena(
                usuario,
                datos.contrasena())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contrasena incorrectos"
            );
        }

        return respuestaDeSesion(usuario, jwtService.generarToken(usuario));
    }

    @Operation(summary = "Renovar la sesión", description = "Requiere un JWT vigente y respeta el límite total de la sesión.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Nuevo JWT y rol actualizado"),
            @ApiResponse(responseCode = "401", description = "Ya no se puede renovar esta sesión", content = @Content)
    })
    @PostMapping("/refresh")
    public AuthResponse renovar(@AuthenticationPrincipal Jwt jwt) {
        // Se consulta la cuenta real para no renovar una cuenta eliminada ni un rol antiguo.
        Number id = jwt.getClaim("id");
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vuelve a iniciar sesión");
        }
        Usuario usuario;
        try {
            usuario = usuarioService.buscarPorId(id.longValue());
        } catch (ResponseStatusException error) {
            if (error.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta ya no está disponible");
            }
            throw error;
        }
        if (!usuario.getCorreo().equals(jwt.getSubject())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vuelve a iniciar sesión");
        }
        return respuestaDeSesion(usuario, jwtService.renovarToken(usuario, jwt));
    }

    private AuthResponse respuestaDeSesion(Usuario usuario, String token) {
        return new AuthResponse(
                token,
                "Bearer",
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidos(),
                usuario.getCorreo(),
                usuario.getRol().name()
        );
    }

    @Operation(summary = "Ver mi perfil", description = "Disponible para cliente y administrador.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Perfil del usuario"),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "El usuario ya no existe", content = @Content)
    })
    @GetMapping("/perfil")
    public UsuarioResponse perfil(
            @AuthenticationPrincipal Jwt jwt) {

        return UsuarioResponse.desde(usuarioService.buscarPorCorreo(
                jwt.getSubject()
        ));
    }
}
