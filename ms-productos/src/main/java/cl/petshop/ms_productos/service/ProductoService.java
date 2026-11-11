
package cl.petshop.ms_productos.service;

import cl.petshop.ms_productos.model.Producto;
import cl.petshop.ms_productos.repository.ProductoRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
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
}
