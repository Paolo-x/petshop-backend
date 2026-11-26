
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.AuthResponse;
import cl.petshop.backend.dto.LoginRequest;
import cl.petshop.backend.dto.RegistroRequest;
import cl.petshop.backend.dto.UsuarioResponse;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.security.JwtService;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthController(
            UsuarioService usuarioService,
            JwtService jwtService) {

        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
    }

    // Registrar un nuevo cliente
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

    // Iniciar sesion
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

        String token = jwtService.generarToken(usuario);

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

    // Consultar el perfil del usuario autenticado
    @GetMapping("/perfil")
    public UsuarioResponse perfil(
            @AuthenticationPrincipal Jwt jwt) {

        return UsuarioResponse.desde(usuarioService.buscarPorCorreo(
                jwt.getSubject()
        ));
    }
}
