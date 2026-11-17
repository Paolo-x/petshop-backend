
package cl.petshop.backend.repository;

import cl.petshop.backend.model.DetallePedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DetallePedidoRepository
        extends JpaRepository<DetallePedido, Long> {

    // Listar los productos de un pedido
    List<DetallePedido> findByPedidoIdOrderByIdAsc(
            Long pedidoId
    );

    // Comprobar si un producto pertenece a algun pedido
    boolean existsByProductoId(Long productoId);
}
