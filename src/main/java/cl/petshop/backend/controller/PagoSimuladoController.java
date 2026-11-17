
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.PedidoResponse;
import cl.petshop.backend.model.Pedido;
import cl.petshop.backend.service.PagoSimuladoService;
import cl.petshop.backend.service.PedidoService;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/pedidos")
public class PagoSimuladoController {

    private final PagoSimuladoService pagoSimuladoService;
    private final PedidoService pedidoService;

    public PagoSimuladoController(
            PagoSimuladoService pagoSimuladoService,
            PedidoService pedidoService) {

        this.pagoSimuladoService = pagoSimuladoService;
        this.pedidoService = pedidoService;
    }

    // Procesar el pago simulado de un pedido
    @PostMapping("/{id}/pagar-simulado")
    @Transactional
    public PedidoResponse procesarPago(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        // El servicio comprueba el cliente,
        // el estado del pedido y el resultado del pago
        Pedido pedido = pagoSimuladoService.procesar(
                id,
                jwt.getSubject()
        );

        // Devolver el resultado con sus productos
        return PedidoResponse.desde(
                pedido,
                pedidoService.listarDetalles(pedido.getId())
        );
    }
}
