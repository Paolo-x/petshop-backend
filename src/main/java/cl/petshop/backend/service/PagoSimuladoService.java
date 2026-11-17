
package cl.petshop.backend.service;

import cl.petshop.backend.model.Pedido;
import cl.petshop.backend.model.Usuario;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@Profile("dev")
public class PagoSimuladoService {

    // Limite utilizado por la pasarela simulada de React
    private static final BigDecimal MONTO_MAXIMO =
            new BigDecimal("100000");

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    @PersistenceContext
    private EntityManager entityManager;

    public PagoSimuladoService(
            PedidoService pedidoService,
            UsuarioService usuarioService) {

        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    // Procesar un pago de prueba
    @Transactional
    public Pedido procesar(
            Long pedidoId,
            String correoAutenticado) {

        // Identificar al cliente autenticado
        Usuario usuario = usuarioService.buscarPorCorreo(
                correoAutenticado
        );

        if (usuario.getRol() != Usuario.Rol.CLIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los clientes pueden procesar sus compras"
            );
        }

        if (pedidoId == null || pedidoId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El ID del pedido no es valido"
            );
        }

        // Bloquear el pedido mientras se procesa
        // para evitar confirmaciones simultaneas
        Pedido pedido = entityManager.find(
                Pedido.class,
                pedidoId,
                LockModeType.PESSIMISTIC_WRITE
        );

        if (pedido == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "El pedido no existe"
            );
        }

        // Comprobar que el pedido pertenece al cliente
        if (!pedido.getCliente().getId()
                .equals(usuario.getId())) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No puedes procesar un pedido de otro cliente"
            );
        }

        // Evitar procesar dos veces el mismo pedido
        if (pedido.getEstado() != Pedido.Estado.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Este pedido ya fue procesado"
            );
        }

        // Validar el total calculado por el backend
        if (pedido.getTotal() == null
                || pedido.getTotal().signum() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El total del pedido no es valido"
            );
        }

        // Simular rechazo por superar el monto permitido
        if (pedido.getTotal().compareTo(MONTO_MAXIMO) > 0) {

            return pedidoService.registrarPagoFallido(
                    pedidoId
            );
        }

        // Simular pago aprobado y descontar stock
        return pedidoService.confirmarPago(
                pedidoId
        );
    }
}
