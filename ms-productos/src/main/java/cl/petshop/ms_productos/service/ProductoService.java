
package cl.petshop.ms_productos.service;

import cl.petshop.ms_productos.dto.ProductoRequest;
import cl.petshop.ms_productos.model.Categoria;
import cl.petshop.ms_productos.model.Producto;
import cl.petshop.ms_productos.repository.CategoriaRepository;
import cl.petshop.ms_productos.repository.ProductoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaRepository categoriaRepository) {

        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // Listar todos los productos
    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    // Buscar un producto por ID
    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {

        return productoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "El producto no existe"
                ));
    }

    // Buscar un producto por codigo
    @Transactional(readOnly = true)
    public Producto buscarPorCodigo(String codigo) {

        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El codigo es obligatorio"
            );
        }

        return productoRepository
                .findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No existe un producto con ese codigo"
                ));
    }

    // Buscar productos por nombre
    @Transactional(readOnly = true)
    public List<Producto> buscarPorNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return listar();
        }

        return productoRepository
                .findByNombreContainingIgnoreCase(nombre.trim());
    }

    // Filtrar productos por categoria
    @Transactional(readOnly = true)
    public List<Producto> buscarPorCategoria(Long categoriaId) {

        return productoRepository.findByCategoriaId(categoriaId);
    }

    // Filtrar por categoria y nombre
    @Transactional(readOnly = true)
    public List<Producto> buscarPorCategoriaYNombre(
            Long categoriaId,
            String nombre) {

        if (nombre == null || nombre.isBlank()) {
            return buscarPorCategoria(categoriaId);
        }

        return productoRepository
                .findByCategoriaIdAndNombreContainingIgnoreCase(
                        categoriaId,
                        nombre.trim()
                );
    }

    // Listar productos con precio de oferta
    @Transactional(readOnly = true)
    public List<Producto> listarOfertas() {

        return productoRepository.findByPrecioOfertaIsNotNull();
    }

    // Listar productos con stock critico
    @Transactional(readOnly = true)
    public List<Producto> listarStockCritico() {

        return productoRepository.buscarProductosConStockCritico();
    }

    // Crear un producto
    @Transactional
    public Producto crear(ProductoRequest request) {

        validarDatosProducto(request);

        String codigo = request.codigo().trim();

        if (productoRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe un producto con ese codigo"
            );
        }

        Categoria categoria = buscarCategoria(request.categoriaId());

        Producto producto = new Producto();

        asignarDatos(producto, request, categoria);

        return productoRepository.save(producto);
    }

    // Actualizar un producto existente
    @Transactional
    public Producto actualizar(Long id, ProductoRequest request) {

        validarDatosProducto(request);

        Producto producto = buscarPorId(id);

        String codigo = request.codigo().trim();

        // Comprobar si el codigo pertenece a otro producto
        productoRepository.findByCodigoIgnoreCase(codigo)
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe otro producto con ese codigo"
                        );
                    }
                });

        Categoria categoria = buscarCategoria(request.categoriaId());

        asignarDatos(producto, request, categoria);

        return productoRepository.save(producto);
    }

    // Eliminar un producto
    @Transactional
    public void eliminar(Long id) {

        Producto producto = buscarPorId(id);

        productoRepository.delete(producto);
    }

    // Buscar y validar la categoria
    private Categoria buscarCategoria(Long categoriaId) {

        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La categoria indicada no existe"
                ));
    }

    // Asignar datos al producto
    private void asignarDatos(
            Producto producto,
            ProductoRequest request,
            Categoria categoria) {

        producto.setCodigo(request.codigo().trim());
        producto.setNombre(request.nombre().trim());
        producto.setDescripcion(request.descripcion());
        producto.setPrecio(request.precio());
        producto.setPrecioOferta(request.precioOferta());
        producto.setStock(request.stock());
        producto.setStockCritico(request.stockCritico());
        producto.setImagenUrl(request.imagenUrl());
        producto.setCategoria(categoria);
    }

    // Validar los datos del producto
    private void validarDatosProducto(ProductoRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del producto son obligatorios"
            );
        }

        if (request.codigo() == null || request.codigo().isBlank()
                || request.nombre() == null || request.nombre().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El codigo y el nombre son obligatorios"
            );
        }

        if (request.precio() == null
                || request.precio().signum() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El precio debe ser mayor que cero"
            );
        }

        if (request.precioOferta() != null
                && (request.precioOferta().signum() <= 0
                || request.precioOferta().compareTo(request.precio()) >= 0)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El precio de oferta debe ser positivo y menor al precio normal"
            );
        }

        if (request.stock() == null || request.stock() < 0
                || request.stockCritico() == null
                || request.stockCritico() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El stock y el stock critico no pueden ser negativos"
            );
        }

        if (request.categoriaId() == null
                || request.categoriaId() <= 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La categoria es obligatoria"
            );
        }
    }
}
