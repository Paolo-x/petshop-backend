
package cl.petshop.backend.controller;

import cl.petshop.backend.dto.CategoriaRequest;
import cl.petshop.backend.model.Categoria;
import cl.petshop.backend.service.CategoriaService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    // Listar todas las categorias
    @GetMapping
    public List<Categoria> listar() {
        return categoriaService.listar();
    }

    // Buscar una categoria por ID
    @GetMapping("/{id}")
    public Categoria buscarPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id);
    }

    // Crear una categoria
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Categoria crear(@Valid @RequestBody CategoriaRequest categoria) {
        return categoriaService.crear(
                categoria.nombre(),
                categoria.imagen()
        );
    }

    // Actualizar una categoria
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

    // Eliminar una categoria
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        categoriaService.eliminar(id);
    }
}
