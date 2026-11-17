
package cl.petshop.backend.service;

import cl.petshop.backend.dto.CrearPedidoRequest;
import cl.petshop.backend.model.DetallePedido;
import cl.petshop.backend.model.Pedido;
import cl.petshop.backend.model.Producto;
import cl.petshop.backend.model.Usuario;
import cl.petshop.backend.repository.DetallePedidoRepository;
import cl.petshop.backend.repository.PedidoRepository;
import cl.petshop.backend.repository.ProductoRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final DetallePedidoRepository detalleRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioService usuarioService;

    @PersistenceContext
    private EntityManager entityManager;

    public PedidoService(
            PedidoRepository pedidoRepository,
            DetallePedidoRepository detalleRepository,
            ProductoRepository productoRepository,
            UsuarioService usuarioService) {

        this.pedidoRepository = pedidoRepository;
        this.detalleRepository = detalleRepository;
        this.productoRepository = productoRepository;
        this.usuarioService = usuarioService;
    }

    // Crear un pedido pendiente
    @Transactional
    public Pedido crear(
            CrearPedidoRequest datos,
            String correoAutenticado) {

        if (datos == null || datos.lineas() == null
                || datos.lineas().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El pedido debe contener productos"
            );
        }

        // Identificar al cliente mediante su sesion JWT
        Usuario cliente = usuarioService.buscarPorCorreo(
                correoAutenticado
        );

        if (cliente.getRol() != Usuario.Rol.CLIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Solo los clientes pueden realizar compras"
            );
        }

        Pedido pedido = new Pedido();

        pedido.setCliente(cliente);
        pedido.setEstado(Pedido.Estado.PENDIENTE);

        // Guardar los datos de entrega
        pedido.setNombreEntrega(datos.nombreEntrega().trim());
        pedido.setApellidosEntrega(datos.apellidosEntrega().trim());
        pedido.setCorreoEntrega(datos.correoEntrega().trim());
        pedido.setCalle(datos.calle().trim());
        pedido.setDepartamento(opcional(datos.departamento()));
        pedido.setRegion(datos.region().trim());
        pedido.setComuna(datos.comuna().trim());
        pedido.setIndicaciones(opcional(datos.indicaciones()));

        // Guardar primero para obtener el ID
        pedido = pedidoRepository.saveAndFlush(pedido);

        // Generar numero de orden
        pedido.setNumero(
                String.format("ORD-%04d", pedido.getId())
        );

        BigDecimal total = BigDecimal.ZERO;

        Set<Long> productosAgregados = new HashSet<>();
        List<DetallePedido> detalles = new ArrayList<>();

        for (CrearPedidoRequest.LineaPedidoRequest linea
                : datos.lineas()) {

            if (linea == null
                    || linea.productoId() == null
                    || linea.cantidad() == null
                    || linea.cantidad() <= 0) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Los datos de un producto no son validos"
                );
            }

            // Evitar productos repetidos
            if (!productosAgregados.add(linea.productoId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "No puedes repetir un producto en el pedido"
                );
            }

            Producto producto = productoRepository
                    .findById(linea.productoId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Uno de los productos no existe"
                    ));

            // Comprobar stock disponible
            if (producto.getStock() < linea.cantidad()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para " + producto.getNombre()
                );
            }

            // Calcular precio con descuento
            BigDecimal precioUnitario =
                    calcularPrecioFinal(producto);

            BigDecimal subtotal = precioUnitario.multiply(
                    BigDecimal.valueOf(linea.cantidad())
            );

            DetallePedido detalle = new DetallePedido();

            detalle.setPedido(pedido);
            detalle.setProducto(producto);
            detalle.setNombreProducto(producto.getNombre());
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotal);

            detalles.add(detalle);

            total = total.add(subtotal);
        }

        // Guardar los detalles y el total
        detalleRepository.saveAll(detalles);

        pedido.setTotal(total);

        return pedidoRepository.save(pedido);
    }

    // Buscar pedido por ID
    @Transactional(readOnly = true)
    public Pedido buscarPorId(Long id) {

        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El pedido no existe"
                ));
    }

    // Listar todos los pedidos
    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAllByOrderByFechaDesc();
    }

    // Listar pedidos de un cliente
    @Transactional(readOnly = true)
    public List<Pedido> listarPorCliente(Long clienteId) {

        return pedidoRepository
                .findByClienteIdOrderByFechaDesc(clienteId);
    }

    // Consultar productos de un pedido
    @Transactional(readOnly = true)
    public List<DetallePedido> listarDetalles(Long pedidoId) {

        buscarPorId(pedidoId);

        return detalleRepository
                .findByPedidoIdOrderByIdAsc(pedidoId);
    }

    // Confirmar un pago y descontar stock
    @Transactional
    public Pedido confirmarPago(Long pedidoId) {

        Pedido pedido = buscarPorId(pedidoId);

        if (pedido.getEstado() != Pedido.Estado.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido ya fue procesado"
            );
        }

        List<DetallePedido> detalles =
                detalleRepository.findByPedidoIdOrderByIdAsc(pedidoId);

        if (detalles.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido no tiene productos"
            );
        }

        // Mantener el mismo orden de bloqueo de productos
        detalles.sort(
                Comparator.comparing(
                        detalle -> detalle.getProducto().getId()
                )
        );

        for (DetallePedido detalle : detalles) {

            // Bloquear el producto durante la actualizacion
            Producto producto = entityManager.find(
                    Producto.class,
                    detalle.getProducto().getId(),
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (producto == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Uno de los productos ya no existe"
                );
            }

            if (producto.getStock() < detalle.getCantidad()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Stock insuficiente para " + producto.getNombre()
                );
            }

            producto.setStock(
                    producto.getStock() - detalle.getCantidad()
            );
        }

        pedido.setEstado(Pedido.Estado.PAGADA);

        return pedidoRepository.save(pedido);
    }

    // Registrar un pago fallido
    @Transactional
    public Pedido registrarPagoFallido(Long pedidoId) {

        Pedido pedido = buscarPorId(pedidoId);

        if (pedido.getEstado() != Pedido.Estado.PENDIENTE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El pedido ya fue procesado"
            );
        }

        pedido.setEstado(Pedido.Estado.FALLIDA);

        return pedidoRepository.save(pedido);
    }

    // Calcular el precio con descuento
    private BigDecimal calcularPrecioFinal(Producto producto) {

        int oferta = producto.getOferta() == null
                ? 0
                : producto.getOferta();

        if (producto.getPrecio() == null
                || producto.getPrecio().signum() <= 0
                || oferta < 0
                || oferta > 100) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El producto tiene un precio u oferta no valido"
            );
        }

        return producto.getPrecio()
                .multiply(BigDecimal.valueOf(100 - oferta))
                .divide(
                        BigDecimal.valueOf(100),
                        0,
                        RoundingMode.HALF_UP
                );
    }

    // Limpiar campos opcionales
    private String opcional(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}
