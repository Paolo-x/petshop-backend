
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.PedidoResponse;
import cl.petshop.backend.model.Pedido;
import cl.petshop.backend.service.PagoSimuladoService;
import cl.petshop.backend.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile("dev")
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pago de prueba", description = "Procesa pagos sin dinero real en el perfil de desarrollo.")
@SecurityRequirement(name = "bearerAuth")
public class PagoSimuladoController {

    private final PagoSimuladoService pagoSimuladoService;
    private final PedidoService pedidoService;

    public PagoSimuladoController(
            PagoSimuladoService pagoSimuladoService,
            PedidoService pedidoService) {

        this.pagoSimuladoService = pagoSimuladoService;
        this.pedidoService = pedidoService;
    }

    @Operation(summary = "Pagar un pedido de forma simulada",
            description = "Solo lo puede pagar su cliente. Si se aprueba, descuenta stock; por sobre $100.000 marca el pago como fallido.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pago procesado: el estado es pagada o fallida"),
                    @ApiResponse(responseCode = "400", description = "Identificador incorrecto", content = @Content),
                    @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
                    @ApiResponse(responseCode = "403", description = "El rol o el dueño no coincide", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content),
                    @ApiResponse(responseCode = "409", description = "Pedido ya procesado o sin stock disponible", content = @Content)
            })
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
