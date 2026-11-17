
package cl.petshop.backend.repository;

import cl.petshop.backend.model.Pedido;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    // Buscar un pedido por su numero
    Optional<Pedido> findByNumero(String numero);

    // Listar pedidos de un cliente, del mas reciente al mas antiguo
    List<Pedido> findByClienteIdOrderByFechaDesc(Long clienteId);

    // Listar todos los pedidos por fecha descendente
    List<Pedido> findAllByOrderByFechaDesc();

    // Buscar pedidos por estado
    List<Pedido> findByEstado(Pedido.Estado estado);
}
