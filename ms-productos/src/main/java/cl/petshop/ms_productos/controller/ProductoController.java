
package cl.petshop.ms_productos.controller;

import cl.petshop.ms_productos.dto.ProductoRequest;
import cl.petshop.ms_productos.dto.ProductoResponse;
import cl.petshop.ms_productos.model.Producto;
import cl.petshop.ms_productos.service.ProductoService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Listar productos y aplicar filtros opcionales
    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Long categoriaId) {

        List<Producto> productos;

        if (categoriaId != null) {
            productos = productoService.buscarPorCategoriaYNombre(
                    categoriaId, nombre
            );
        } else {
            productos = productoService.buscarPorNombre(nombre);
        }

        return productos.stream()
                .map(ProductoResponse::desdeEntidad)
                .toList();
    }

    // Buscar un producto por ID
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ProductoResponse buscarPorId(@PathVariable Long id) {

        return ProductoResponse.desdeEntidad(
                productoService.buscarPorId(id)
        );
    }

    // Buscar un producto por codigo
    @GetMapping("/codigo/{codigo}")
    @Transactional(readOnly = true)
    public ProductoResponse buscarPorCodigo(
            @PathVariable String codigo) {

        return ProductoResponse.desdeEntidad(
                productoService.buscarPorCodigo(codigo)
        );
    }

    // Listar productos con ofertas
    @GetMapping("/ofertas")
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarOfertas() {

        return productoService.listarOfertas()
                .stream()
                .map(ProductoResponse::desdeEntidad)
                .toList();
    }

    // Listar productos con stock critico
    @GetMapping("/stock-critico")
    @Transactional(readOnly = true)
    public List<ProductoResponse> listarStockCritico() {

        return productoService.listarStockCritico()
                .stream()
                .map(ProductoResponse::desdeEntidad)
                .toList();
    }

    // Crear un producto
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductoResponse crear(
            @Valid @RequestBody ProductoRequest request) {

        return ProductoResponse.desdeEntidad(
                productoService.crear(request)
        );
    }

    // Actualizar un producto
    @PutMapping("/{id}")
    @Transactional
    public ProductoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {

        return ProductoResponse.desdeEntidad(
                productoService.actualizar(id, request)
        );
    }

    // Eliminar un producto
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {

        productoService.eliminar(id);
    }
}
