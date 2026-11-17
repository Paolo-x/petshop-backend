
package cl.petshop.backend.dto;

public record AuthResponse(

        String token,

        String tipo,

        Long id,

        String nombre,

        String apellidos,

        String correo,

        String rol

) {
}
