
package cl.petshop.ms_productos.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoRequest(

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 40, message = "El codigo no puede superar los 40 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio debe tener como maximo 10 digitos enteros y 2 decimales")
        BigDecimal precio,

        @DecimalMin(value = "0.01", message = "El precio de oferta debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El precio de oferta debe tener como maximo 10 digitos enteros y 2 decimales")
        BigDecimal precioOferta,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock,

        @NotNull(message = "El stock critico es obligatorio")
        @Min(value = 0, message = "El stock critico no puede ser negativo")
        Integer stockCritico,

        @Size(max = 500, message = "La URL de la imagen no puede superar los 500 caracteres")
        String imagenUrl,

        @NotNull(message = "La categoria es obligatoria")
        @Positive(message = "El ID de la categoria debe ser positivo")
        Long categoriaId

) {

    // El precio de oferta debe ser menor al precio normal
    @AssertTrue(message = "El precio de oferta debe ser menor al precio normal")
    public boolean isPrecioOfertaValido() {

        if (precioOferta == null || precio == null) {
            return true;
        }

        return precioOferta.compareTo(precio) < 0;
    }
}
