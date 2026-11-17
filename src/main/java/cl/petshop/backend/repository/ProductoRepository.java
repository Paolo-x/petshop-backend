
package cl.petshop.backend.repository;

import cl.petshop.backend.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    // Buscar producto por codigo
    Optional<Producto> findByCodigoIgnoreCase(String codigo);

    // Comprobar si existe un codigo
    boolean existsByCodigoIgnoreCase(String codigo);

    // Listar productos de una categoria
    List<Producto> findByCategoriaId(Long categoriaId);

    // Comprobar si una categoria tiene productos
    boolean existsByCategoriaId(Long categoriaId);

    // Buscar productos por nombre
    List<Producto> findByNombreContainingIgnoreCase(String nombre);
}
