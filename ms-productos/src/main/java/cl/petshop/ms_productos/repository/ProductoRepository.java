
package cl.petshop.ms_productos.repository;

import cl.petshop.ms_productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar un producto por su codigo
    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    // Comprobar si existe un codigo
    boolean existsByCodigoIgnoreCase(String codigo);

    // Buscar productos por nombre
    List<Producto> findByNombreContainingIgnoreCase(String nombre);

    // Buscar productos por categoria
    List<Producto> findByCategoriaId(Long categoriaId);

    // Buscar productos por categoria y nombre
    List<Producto> findByCategoriaIdAndNombreContainingIgnoreCase(
            Long categoriaId, String nombre
    );

    // Buscar productos que tienen precio de oferta
    List<Producto> findByPrecioOfertaIsNotNull();

    // Buscar productos con stock critico
    @Query("SELECT p FROM Producto p WHERE p.stock <= p.stockCritico")
    List<Producto> buscarProductosConStockCritico();

    // Comprobar si una categoria tiene productos asociados
    boolean existsByCategoriaId(Long categoriaId);

}
