
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.ProductoRequest;
import cl.petshop.backend.model.Producto;
import cl.petshop.backend.service.ProductoService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Consultas del catálogo y mantenimiento por administración.")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @Operation(summary = "Listar productos", responses = @ApiResponse(responseCode = "200", description = "Listado público"))
    @GetMapping
    public List<Producto> listar() {
        return productoService.listar();
    }

    @Operation(summary = "Ver el detalle de un producto", responses = {
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public Producto buscarPorId(@PathVariable Long id) {
        return productoService.buscarPorId(id);
    }

    @Operation(summary = "Listar productos por categoría", responses = {
            @ApiResponse(responseCode = "200", description = "Productos de la categoría"),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content)
    })
    @GetMapping("/categoria/{categoriaId}")
    public List<Producto> listarPorCategoria(
            @PathVariable Long categoriaId) {

        return productoService.listarPorCategoria(categoriaId);
    }

    @Operation(summary = "Buscar productos por nombre", description = "Sin nombre, devuelve el listado completo.",
            responses = @ApiResponse(responseCode = "200", description = "Resultados de la búsqueda"))
    @GetMapping("/buscar")
    public List<Producto> buscarPorNombre(
            @RequestParam(required = false) String nombre) {

        return productoService.buscarPorNombre(nombre);
    }

    @Operation(summary = "Crear un producto", description = "Solo para administración.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(responseCode = "400", description = "Datos del producto incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "La categoría no existe", content = @Content),
            @ApiResponse(responseCode = "409", description = "Código de producto repetido", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Producto crear(@Valid @RequestBody ProductoRequest producto) {
        return productoService.crear(producto);
    }

    @Operation(summary = "Actualizar un producto", description = "Solo para administración.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "400", description = "Identificador o datos incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "El producto o la categoría no existe", content = @Content),
            @ApiResponse(responseCode = "409", description = "Código de producto repetido", content = @Content)
    })
    @PutMapping("/{id}")
    public Producto actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoRequest producto) {

        return productoService.actualizar(id, producto);
    }

    @Operation(summary = "Eliminar un producto", description = "Solo para administración.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "204", description = "Producto eliminado", content = @Content),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Producto no encontrado", content = @Content),
            @ApiResponse(responseCode = "409", description = "El producto está vinculado a un pedido", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productoService.eliminar(id);
    }
}
