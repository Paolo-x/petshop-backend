
package cl.petshop.backend.dto;

import cl.petshop.backend.model.DetallePedido;
import cl.petshop.backend.model.Pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

public record PedidoResponse(

        Long id,
        String numero,
        Instant fecha,
        String estado,
        BigDecimal total,
        Long clienteId,
        DatosEntrega cliente,
        List<Linea> lineas

) {

    // Datos de entrega del pedido
    public record DatosEntrega(

            String nombre,
            String apellidos,
            String correo,
            String calle,
            String departamento,
            String region,
            String comuna,
            String indicaciones

    ) {
    }

    // Producto incluido en la compra
    public record Linea(

            Long id,
            String nombre,
            Integer cantidad,
            BigDecimal unitario,
            BigDecimal subtotal

    ) {
    }

    // Convertir las entidades en una respuesta para React
    public static PedidoResponse desde(
            Pedido pedido,
            List<DetallePedido> detalles) {

        DatosEntrega cliente = new DatosEntrega(
                pedido.getNombreEntrega(),
                pedido.getApellidosEntrega(),
                pedido.getCorreoEntrega(),
                pedido.getCalle(),
                pedido.getDepartamento(),
                pedido.getRegion(),
                pedido.getComuna(),
                pedido.getIndicaciones()
        );

        List<Linea> lineas = detalles.stream()
                .map(detalle -> new Linea(
                        detalle.getProducto().getId(),
                        detalle.getNombreProducto(),
                        detalle.getCantidad(),
                        detalle.getPrecioUnitario(),
                        detalle.getSubtotal()
                ))
                .toList();

        return new PedidoResponse(
                pedido.getId(),
                pedido.getNumero(),
                pedido.getFecha(),
                pedido.getEstado().name()
                        .toLowerCase(Locale.ROOT),
                pedido.getTotal(),
                pedido.getCliente().getId(),
                cliente,
                lineas
        );
    }
}
