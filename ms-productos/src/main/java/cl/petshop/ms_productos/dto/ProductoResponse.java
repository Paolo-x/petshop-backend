
package cl.petshop.ms_productos.dto;

import cl.petshop.ms_productos.model.Producto;

import java.math.BigDecimal;

public record ProductoResponse(

        Long id,
        String codigo,
        String nombre,
        String descripcion,
        BigDecimal precio,
        BigDecimal precioOferta,
        Integer stock,
        Integer stockCritico,
        String imagenUrl,
        Long categoriaId,
        String categoriaNombre

) {

    // Convertir una entidad Producto en un DTO
    public static ProductoResponse desdeEntidad(Producto producto) {

        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getPrecio(),
                producto.getPrecioOferta(),
                producto.getStock(),
                producto.getStockCritico(),
                producto.getImagenUrl(),
                producto.getCategoria().getId(),
                producto.getCategoria().getNombre()
        );
    }
}
