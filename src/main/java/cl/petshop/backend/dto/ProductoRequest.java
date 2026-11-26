package cl.petshop.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductoRequest(
        @NotBlank @Size(min = 3, max = 50) String codigo,
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        @NotNull @Valid CategoriaReferencia categoria,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precio,
        @Min(0) @Max(100) Integer oferta,
        @NotNull @PositiveOrZero Integer stock,
        @PositiveOrZero Integer stockCritico,
        @Size(max = 500) String imagen
) {
    public ProductoRequest {
        codigo = codigo == null ? null : codigo.trim();
        nombre = nombre == null ? null : nombre.trim();
        precio = precio == null ? null : precio.stripTrailingZeros();
    }

    public record CategoriaReferencia(@NotNull @Positive Long id) {
    }
}
