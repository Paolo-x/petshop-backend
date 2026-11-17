
package cl.petshop.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrearPedidoRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 50)
        String nombreEntrega,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100)
        String apellidosEntrega,

        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "El correo no es valido")
        @Size(max = 100)
        String correoEntrega,

        @NotBlank(message = "La calle es obligatoria")
        @Size(max = 200)
        String calle,

        @Size(max = 100)
        String departamento,

        @NotBlank(message = "La region es obligatoria")
        @Size(max = 100)
        String region,

        @NotBlank(message = "La comuna es obligatoria")
        @Size(max = 100)
        String comuna,

        @Size(max = 500)
        String indicaciones,

        @NotEmpty(message = "El pedido debe tener productos")
        List<@Valid LineaPedidoRequest> lineas

) {

    // Datos de cada producto solicitado
    public record LineaPedidoRequest(

            @NotNull(message = "El producto es obligatorio")
            @Positive(message = "El ID del producto debe ser positivo")
            Long productoId,

            @NotNull(message = "La cantidad es obligatoria")
            @Positive(message = "La cantidad debe ser mayor que cero")
            Integer cantidad

    ) {
    }
}
