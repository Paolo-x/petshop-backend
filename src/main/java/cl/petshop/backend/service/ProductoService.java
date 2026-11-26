
package cl.petshop.backend.service;

import cl.petshop.backend.dto.ProductoRequest;
import cl.petshop.backend.model.Categoria;
import cl.petshop.backend.model.Producto;
import cl.petshop.backend.repository.DetallePedidoRepository;
import cl.petshop.backend.repository.ProductoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaService categoriaService;
    private final DetallePedidoRepository detallePedidoRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaService categoriaService,
            DetallePedidoRepository detallePedidoRepository) {

        this.productoRepository = productoRepository;
        this.categoriaService = categoriaService;
        this.detallePedidoRepository = detallePedidoRepository;
    }

    // Listar todos los productos
    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    // Buscar producto por ID
    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El producto no existe"
                ));
    }

    // Listar productos por categoria
    @Transactional(readOnly = true)
    public List<Producto> listarPorCategoria(Long categoriaId) {
        categoriaService.buscarPorId(categoriaId);
        return productoRepository.findByCategoriaId(categoriaId);
    }

    // Buscar productos por nombre
    @Transactional(readOnly = true)
    public List<Producto> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return listar();
        }

        return productoRepository.findByNombreContainingIgnoreCase(
                nombre.trim()
        );
    }

    // Crear producto
    @Transactional
    public Producto crear(ProductoRequest datos) {

        validarCodigo(datos.codigo(), null);

        Categoria categoria = categoriaService.buscarPorId(
                datos.categoria().id()
        );

        Producto producto = new Producto();
        copiarDatos(producto, datos, categoria);

        return productoRepository.save(producto);
    }

    // Actualizar producto
    @Transactional
    public Producto actualizar(Long id, ProductoRequest datos) {

        Producto producto = buscarPorId(id);

        validarCodigo(datos.codigo(), id);

        Categoria categoria = categoriaService.buscarPorId(
                datos.categoria().id()
        );

        copiarDatos(producto, datos, categoria);

        return productoRepository.save(producto);
    }

    // Eliminar producto
    @Transactional
    public void eliminar(Long id) {

        Producto producto = buscarPorId(id);

        if (detallePedidoRepository.existsByProductoId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar un producto que figura en un pedido");
        }
        productoRepository.delete(producto);
    }

    // Evitar codigos duplicados
    private void validarCodigo(String codigo, Long idActual) {

        productoRepository.findByCodigoIgnoreCase(codigo.trim())
                .ifPresent(existente -> {

                    if (idActual == null ||
                            !existente.getId().equals(idActual)) {

                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe un producto con ese codigo"
                        );
                    }
                });
    }

    // Copiar los datos validados
    private void copiarDatos(
            Producto producto,
            ProductoRequest datos,
            Categoria categoria) {

        producto.setCodigo(datos.codigo());
        producto.setNombre(datos.nombre());
        producto.setDescripcion(limpiarTexto(datos.descripcion()));
        producto.setPrecio(datos.precio());
        producto.setOferta(
                datos.oferta() == null ? 0 : datos.oferta()
        );
        producto.setStock(datos.stock());
        producto.setStockCritico(
                datos.stockCritico() == null ? 0 : datos.stockCritico()
        );
        producto.setImagen(limpiarTexto(datos.imagen()));
        producto.setCategoria(categoria);
    }

    // Limpiar textos opcionales
    private String limpiarTexto(String texto) {

        if (texto == null || texto.isBlank()) {
            return null;
        }

        return texto.trim();
    }
}
