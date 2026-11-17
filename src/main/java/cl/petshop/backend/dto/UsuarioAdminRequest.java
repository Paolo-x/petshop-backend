
package cl.petshop.backend.dto;

import cl.petshop.backend.model.Usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UsuarioAdminRequest(

        @NotBlank(message = "El RUN es obligatorio")
        @Size(max = 20, message = "El RUN no puede superar los 20 caracteres")
        String run,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50, message = "El nombre no puede superar 50 caracteres")
        String nombre,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
        String apellidos,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 100, message = "El correo no puede superar 100 caracteres")
        String correo,

        @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
        String contrasena,

        @NotNull(message = "El rol es obligatorio")
        Usuario.Rol rol

) {
}
