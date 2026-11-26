
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.ProductoRequest;
import cl.petshop.backend.model.Producto;
import cl.petshop.backend.service.ProductoService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    // Listar todos los productos
    @GetMapping
    public List<Producto> listar() {
        return productoService.listar();
    }

    // Buscar producto por ID
    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    // Listar productos por categoria
    @GetMapping("/categoria/{categoriaId}")
    public List<Producto> listarPorCategoria(
            @PathVariable Long categoriaId) {

        return productoService.listarPorCategoria(categoriaId);
    }

    // Buscar productos por nombre
    @GetMapping("/buscar")
    public List<Producto> buscarPorNombre(
            @RequestParam(required = false) String nombre) {

        return productoService.buscarPorNombre(nombre);
    }

    // Crear producto
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody ProductoRequest producto) {
        return productoService.crear(producto);
    }

    // Actualizar producto
    @PutMapping("/{id}")
    public Producto actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest producto) {

        return productoService.actualizar(id, producto);
    }

    // Eliminar producto
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
