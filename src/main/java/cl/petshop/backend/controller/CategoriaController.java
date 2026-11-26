
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.CategoriaRequest;
import cl.petshop.backend.model.Categoria;
import cl.petshop.backend.service.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@Tag(name = "Categorías", description = "Consultas de la tienda y mantenimiento por administración.")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Operation(summary = "Listar categorías", description = "Ruta pública de la tienda.", responses = {
            @ApiResponse(responseCode = "200", description = "Listado de categorías"),
            @ApiResponse(responseCode = "403", description = "La sesión del vendedor no tiene acceso", content = @Content)
    })
    @GetMapping
    public List<Categoria> listar() {
        return categoriaService.listar();
    }

    @Operation(summary = "Ver una categoría", description = "Ruta pública de la tienda.", responses = {
            @ApiResponse(responseCode = "200", description = "Categoría encontrada"),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "403", description = "La sesión del vendedor no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content)
    })
    @GetMapping("/{id}")
    public Categoria buscarPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id);
    }

    @Operation(summary = "Crear una categoría", description = "Solo para administración.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "201", description = "Categoría creada"),
            @ApiResponse(responseCode = "400", description = "Datos de la categoría incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nombre de categoría repetido", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria crear(@Valid @RequestBody CategoriaRequest categoria) {
        return categoriaService.crear(
                categoria.nombre(),
                categoria.imagen()
        );
    }

    @Operation(summary = "Actualizar una categoría", description = "Solo para administración.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "200", description = "Categoría actualizada"),
            @ApiResponse(responseCode = "400", description = "Identificador o datos incorrectos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "Nombre de categoría repetido", content = @Content)
    })
    @PutMapping("/{id}")
    public Categoria actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest categoria) {

        return categoriaService.actualizar(
                id,
                categoria.nombre(),
                categoria.imagen()
        );
    }

    @Operation(summary = "Eliminar una categoría", description = "Solo para administración.",
            security = @SecurityRequirement(name = "bearerAuth"), responses = {
            @ApiResponse(responseCode = "204", description = "Categoría eliminada", content = @Content),
            @ApiResponse(responseCode = "400", description = "El identificador no es numérico", content = @Content),
            @ApiResponse(responseCode = "401", description = "Falta una sesión válida", content = @Content),
            @ApiResponse(responseCode = "403", description = "El rol no tiene acceso", content = @Content),
            @ApiResponse(responseCode = "404", description = "Categoría no encontrada", content = @Content),
            @ApiResponse(responseCode = "409", description = "La categoría tiene productos asignados", content = @Content)
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}
