
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.CrearPedidoRequest;
import cl.petshop.backend.dto.PedidoResponse;
import cl.petshop.backend.model.Pedido;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.service.PedidoService;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
@Tag(name = "Pedidos", description = "Creación y consulta de pedidos con sus productos.")
@SecurityRequirement(name = "bearerAuth")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PedidoController(
            PedidoService pedidoService,
            UsuarioService usuarioService) {

        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    @Operation(summary = "Crear un pedido", description = "El cliente crea un pedido pendiente; aún no se descuenta stock.", responses = {
            @ApiResponse(responseCode = "201", description = "Pedido pendiente creado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrega o líneas incorrectas", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "No se encontró un producto", content = @Content),
            @ApiResponse(responseCode = "409", description = "Stock insuficiente o datos del producto no válidos", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public PedidoResponse crear(
            @Valid @RequestBody CrearPedidoRequest datos,
            @AuthenticationPrincipal Jwt jwt) {

        Pedido pedido = pedidoService.crear(
                datos,
                jwt.getSubject()
        );

        return convertirRespuesta(pedido);
    }

    @Operation(summary = "Listar todos los pedidos", description = "Solo para vendedores y administradores.", responses = {
            @ApiResponse(responseCode = "200", description = "Listado de pedidos"),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content)
    })
    @GetMapping
    @Transactional(readOnly = true)
    public List<PedidoResponse> listar(
            @AuthenticationPrincipal Jwt jwt) {

        Usuario usuario = obtenerUsuario(jwt);
        comprobarPersonal(usuario);

        return pedidoService.listar()
                .stream()
                .map(this::convertirRespuesta)
                .toList();
    }

    @Operation(summary = "Ver mis pedidos", description = "Muestra solo los pedidos del cliente autenticado.", responses = {
            @ApiResponse(responseCode = "200", description = "Pedidos del cliente"),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "Solo para clientes", content = @Content)
    })
    @GetMapping("/mis-pedidos")
    @Transactional(readOnly = true)
    public List<PedidoResponse> misPedidos(
            @AuthenticationPrincipal Jwt jwt) {

        Usuario usuario = obtenerUsuario(jwt);

        if (usuario.getRol() != Usuario.Rol.CLIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Esta consulta es solo para clientes"
            );
        }

        return pedidoService.listarPorCliente(
                usuario.getId()
        ).stream()
         .map(this::convertirRespuesta)
         .toList();
    }

    @Operation(summary = "Ver el detalle de un pedido",
            description = "Disponible para el cliente dueño del pedido, vendedores y administradores.", responses = {
            @ApiResponse(responseCode = "200", description = "Pedido y sus líneas"),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El pedido pertenece a otro cliente", content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PedidoResponse buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {

        Usuario usuario = obtenerUsuario(jwt);
        Pedido pedido = pedidoService.buscarPorId(id);

        boolean esPersonal =
                usuario.getRol() == Usuario.Rol.ADMINISTRADOR
                || usuario.getRol() == Usuario.Rol.VENDEDOR;

        boolean esPropietario =
                pedido.getCliente().getId().equals(usuario.getId());

        if (!esPersonal && !esPropietario) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para consultar este pedido"
            );
        }

        return convertirRespuesta(pedido);
    }

    // Buscar el usuario que inicio sesion
    private Usuario obtenerUsuario(Jwt jwt) {

        return usuarioService.buscarPorCorreo(
                jwt.getSubject()
        );
    }

    // Comprobar permisos de administracion y ventas
    private void comprobarPersonal(Usuario usuario) {

        if (usuario.getRol() != Usuario.Rol.ADMINISTRADOR
                && usuario.getRol() != Usuario.Rol.VENDEDOR) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "No tienes permiso para consultar todos los pedidos"
            );
        }
    }

    // Preparar los datos para React
    private PedidoResponse convertirRespuesta(Pedido pedido) {

        return PedidoResponse.desde(
                pedido,
                pedidoService.listarDetalles(pedido.getId())
        );
    }
}
