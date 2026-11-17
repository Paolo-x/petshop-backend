
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.AuthResponse;
import cl.petshop.backend.dto.LoginRequest;
import cl.petshop.backend.dto.RegistroRequest;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.security.JwtService;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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
    public Usuario registrar(
            @Valid @RequestBody RegistroRequest datos) {

        Usuario usuario = new Usuario();

        usuario.setRun(datos.run());
        usuario.setNombre(datos.nombre());
        usuario.setApellidos(datos.apellidos());
        usuario.setCorreo(datos.correo());

        // El servicio asigna siempre el rol CLIENTE
        return usuarioService.registrarCliente(
                usuario,
                datos.contrasena()
        );
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

        // Comprobar la contrasena con BCrypt
        if (!usuarioService.comprobarContrasena(
                usuario,
                datos.contrasena())) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Correo o contrasena incorrectos"
            );
        }

        // Generar el token JWT
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
}
