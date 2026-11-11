
package cl.petshop.ms_productos.dto;

import cl.petshop.ms_productos.model.Categoria;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        String color
) {

    public static CategoriaResponse desdeEntidad(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.getColor()
        );
    }
}
