
package cl.petshop.ms_productos.controller;

import cl.petshop.ms_productos.dto.CategoriaRequest;
import cl.petshop.ms_productos.dto.CategoriaResponse;
import cl.petshop.ms_productos.service.CategoriaService;

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
    public List<CategoriaResponse> listar() {

        return categoriaService.listar()
                .stream()
                .map(CategoriaResponse::desdeEntidad)
                .toList();
    }

    // Buscar una categoria por ID
    @GetMapping("/{id}")
    public CategoriaResponse buscarPorId(@PathVariable Long id) {

        return CategoriaResponse.desdeEntidad(
                categoriaService.buscarPorId(id)
        );
    }

    // Crear una categoria
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaResponse crear(
            @Valid @RequestBody CategoriaRequest request) {

        return CategoriaResponse.desdeEntidad(
                categoriaService.crear(
                        request.nombre(),
                        request.descripcion(),
                        request.color()
                )
        );
    }

    // Actualizar una categoria
    @PutMapping("/{id}")
    public CategoriaResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest request) {

        return CategoriaResponse.desdeEntidad(
                categoriaService.actualizar(
                        id,
                        request.nombre(),
                        request.descripcion(),
                        request.color()
                )
        );
    }

    // Eliminar una categoria
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {

        categoriaService.eliminar(id);
    }
}
