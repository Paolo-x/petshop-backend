
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.CrearPedidoRequest;
import cl.petshop.backend.dto.PedidoResponse;
import cl.petshop.backend.model.Pedido;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.service.PedidoService;
import cl.petshop.backend.service.UsuarioService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;
    private final UsuarioService usuarioService;

    public PedidoController(
            PedidoService pedidoService,
            UsuarioService usuarioService) {

        this.pedidoService = pedidoService;
        this.usuarioService = usuarioService;
    }

    // Crear un pedido como cliente autenticado
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

    // Listar todos los pedidos
    // Solo administradores y vendedores
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

    // Consultar las compras del cliente autenticado
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

    // Consultar un pedido por ID
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
