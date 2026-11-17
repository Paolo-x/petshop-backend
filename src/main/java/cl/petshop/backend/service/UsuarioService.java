
package cl.petshop.backend.service;

import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.repository.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    // Listar usuarios
    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    // Buscar usuario por ID
    @Transactional(readOnly = true)
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no existe"
                ));
    }

    // Buscar usuario por correo
    @Transactional(readOnly = true)
    public Usuario buscarPorCorreo(String correo) {

        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo es obligatorio"
            );
        }

        return usuarioRepository
                .findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El usuario no existe"
                ));
    }

    // Registro publico: siempre crea un CLIENTE
    @Transactional
    public Usuario registrarCliente(
            Usuario datos,
            String contrasena) {

        return crearUsuario(
                datos,
                contrasena,
                Usuario.Rol.CLIENTE
        );
    }

    // Crear usuario desde administracion
    // Solo debe exponerse en una ruta protegida para ADMINISTRADOR
    @Transactional
    public Usuario crearPorAdministrador(
            Usuario datos,
            String contrasena) {

        if (datos == null || datos.getRol() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El rol es obligatorio"
            );
        }

        return crearUsuario(
                datos,
                contrasena,
                datos.getRol()
        );
    }

    // Crear y guardar un usuario
    private Usuario crearUsuario(
            Usuario datos,
            String contrasena,
            Usuario.Rol rol) {

        validarDatos(datos);
        validarContrasena(contrasena);
        validarUnicos(datos, null);

        Usuario usuario = new Usuario();

        copiarDatos(usuario, datos);
        usuario.setContrasenaHash(
                passwordEncoder.encode(contrasena)
        );
        usuario.setRol(rol);

        return usuarioRepository.save(usuario);
    }

    // Actualizar usuario desde administracion
    // Solo debe exponerse en una ruta protegida para ADMINISTRADOR
    @Transactional
    public Usuario actualizarPorAdministrador(
            Long id,
            Usuario datos,
            String nuevaContrasena) {

        Usuario usuario = buscarPorId(id);

        validarDatos(datos);
        validarUnicos(datos, id);

        if (datos.getRol() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El rol es obligatorio"
            );
        }

        copiarDatos(usuario, datos);
        usuario.setRol(datos.getRol());

        // Contrasena opcional durante la edicion
        if (nuevaContrasena != null &&
                !nuevaContrasena.isBlank()) {

            validarContrasena(nuevaContrasena);

            usuario.setContrasenaHash(
                    passwordEncoder.encode(nuevaContrasena)
            );
        }

        return usuarioRepository.save(usuario);
    }

    // Eliminar usuario
    // Solo debe exponerse en una ruta protegida para ADMINISTRADOR
    @Transactional
    public void eliminar(Long id) {

        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }

    // Comprobar contrasena cifrada
    public boolean comprobarContrasena(
            Usuario usuario,
            String contrasena) {

        if (usuario == null || contrasena == null) {
            return false;
        }

        return passwordEncoder.matches(
                contrasena,
                usuario.getContrasenaHash()
        );
    }

    // Validar datos personales
    private void validarDatos(Usuario datos) {

        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos son obligatorios"
            );
        }

        String run = datos.getRun();
        String nombre = datos.getNombre();
        String apellidos = datos.getApellidos();
        String correo = datos.getCorreo();

        if (run == null ||
                run.isBlank() ||
                run.trim().length() > 20) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El RUN es obligatorio y no puede superar 20 caracteres"
            );
        }

        if (nombre == null ||
                nombre.isBlank() ||
                nombre.trim().length() > 50) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio y no puede superar 50 caracteres"
            );
        }

        if (apellidos == null ||
                apellidos.isBlank() ||
                apellidos.trim().length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los apellidos son obligatorios y no pueden superar 100 caracteres"
            );
        }

        if (correo == null ||
                correo.isBlank() ||
                correo.trim().length() > 100 ||
                !correo.trim().matches(
                        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
                )) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El correo electronico no es valido"
            );
        }
    }

    // Validar contrasena
    private void validarContrasena(String contrasena) {

        if (contrasena == null ||
                contrasena.length() < 8 ||
                contrasena.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La contrasena debe tener al menos 8 caracteres y no superar 72 bytes"
            );
        }
    }

    // Evitar correos y RUN duplicados
    private void validarUnicos(
            Usuario datos,
            Long idActual) {

        String correo = datos.getCorreo().trim();

        usuarioRepository.findByCorreoIgnoreCase(correo)
                .ifPresent(existente -> {

                    if (!existente.getId().equals(idActual)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe un usuario con ese correo"
                        );
                    }
                });

        String run = datos.getRun().trim();

        usuarioRepository.findByRun(run)
                .ifPresent(existente -> {

                    if (!existente.getId().equals(idActual)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe un usuario con ese RUN"
                        );
                    }
                });
    }

    // Copiar datos personales
    private void copiarDatos(
            Usuario usuario,
            Usuario datos) {

        usuario.setRun(datos.getRun().trim());
        usuario.setNombre(datos.getNombre().trim());
        usuario.setApellidos(datos.getApellidos().trim());

        usuario.setCorreo(
                datos.getCorreo().trim().toLowerCase(Locale.ROOT)
        );
    }
}
