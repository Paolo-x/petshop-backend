
package cl.petshop.backend.service;

import cl.petshop.backend.model.Categoria;
import cl.petshop.backend.model.Producto;
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

    public ProductoService(
            ProductoRepository productoRepository,
            CategoriaService categoriaService) {

        this.productoRepository = productoRepository;
        this.categoriaService = categoriaService;
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
    public Producto crear(Producto datos) {

        validarDatos(datos);
        validarCodigo(datos.getCodigo(), null);

        Categoria categoria = categoriaService.buscarPorId(
                datos.getCategoria().getId()
        );

        Producto producto = new Producto();
        copiarDatos(producto, datos, categoria);

        return productoRepository.save(producto);
    }

    // Actualizar producto
    @Transactional
    public Producto actualizar(Long id, Producto datos) {

        Producto producto = buscarPorId(id);

        validarDatos(datos);
        validarCodigo(datos.getCodigo(), id);

        Categoria categoria = categoriaService.buscarPorId(
                datos.getCategoria().getId()
        );

        copiarDatos(producto, datos, categoria);

        return productoRepository.save(producto);
    }

    // Eliminar producto
    @Transactional
    public void eliminar(Long id) {

        Producto producto = buscarPorId(id);

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

    // Validar datos del producto
    private void validarDatos(Producto datos) {

        if (datos == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Los datos del producto son obligatorios"
            );
        }

        if (datos.getCodigo() == null ||
                datos.getCodigo().trim().length() < 3 ||
                datos.getCodigo().trim().length() > 50) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El codigo debe tener entre 3 y 50 caracteres"
            );
        }

        if (datos.getNombre() == null ||
                datos.getNombre().isBlank() ||
                datos.getNombre().trim().length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre es obligatorio y no puede superar 100 caracteres"
            );
        }

        if (datos.getDescripcion() != null &&
                datos.getDescripcion().length() > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La descripcion no puede superar 500 caracteres"
            );
        }

        if (datos.getPrecio() == null ||
                datos.getPrecio().compareTo(BigDecimal.ZERO) <= 0 ||
                datos.getPrecio().stripTrailingZeros().scale() > 2 ||
                datos.getPrecio().stripTrailingZeros().precision()
                - datos.getPrecio().stripTrailingZeros().scale() > 10) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El precio debe ser positivo y tener maximo 2 decimales"
            );
        }

        if (datos.getOferta() != null &&
                (datos.getOferta() < 0 || datos.getOferta() > 100)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La oferta debe estar entre 0 y 100"
            );
        }

        if (datos.getStock() == null || datos.getStock() < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El stock no puede ser negativo"
            );
        }

        if (datos.getStockCritico() != null &&
                datos.getStockCritico() < 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El stock critico no puede ser negativo"
            );
        }

        if (datos.getImagen() != null &&
                datos.getImagen().length() > 500) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La ruta de imagen no puede superar 500 caracteres"
            );
        }

        if (datos.getCategoria() == null ||
                datos.getCategoria().getId() == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Debes seleccionar una categoria"
            );
        }
    }

    // Copiar los datos validados
    private void copiarDatos(
            Producto producto,
            Producto datos,
            Categoria categoria) {

        producto.setCodigo(datos.getCodigo().trim());
        producto.setNombre(datos.getNombre().trim());
        producto.setDescripcion(limpiarTexto(datos.getDescripcion()));
        producto.setPrecio(datos.getPrecio());
        producto.setOferta(
                datos.getOferta() == null ? 0 : datos.getOferta()
        );
        producto.setStock(datos.getStock());
        producto.setStockCritico(
                datos.getStockCritico() == null ? 0 : datos.getStockCritico()
        );
        producto.setImagen(limpiarTexto(datos.getImagen()));
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
