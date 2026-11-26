package cl.petshop.backend.dto;

import cl.petshop.backend.model.Usuario;

// Solo estos datos salen en las respuestas. La clave y su hash quedan fuera.
public record UsuarioResponse(
        Long id, String run, String nombre, String apellidos, String correo, Usuario.Rol rol
) {
    public static UsuarioResponse desde(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getRun(), usuario.getNombre(),
                usuario.getApellidos(), usuario.getCorreo(), usuario.getRol());
    }
}
