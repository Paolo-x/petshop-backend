
package cl.petshop.ms_productos.service;

import cl.petshop.ms_productos.model.Categoria;
import cl.petshop.ms_productos.repository.CategoriaRepository;
import cl.petshop.ms_productos.repository.ProductoRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaService(
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository) {

        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    // Listar todas las categorias
    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return categoriaRepository.findAll();
    }

    // Buscar una categoria por ID
    @Transactional(readOnly = true)
    public Categoria buscarPorId(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La categoria no existe"
                ));
    }

    // Crear una categoria
    @Transactional
    public Categoria crear(
            String nombre,
            String descripcion,
            String color) {

        String nombreLimpio = validarNombre(nombre);
        validarOtrosCampos(descripcion, color);

        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una categoria con ese nombre"
            );
        }

        Categoria categoria = new Categoria();
        categoria.setNombre(nombreLimpio);
        categoria.setDescripcion(descripcion);
        categoria.setColor(color);

        return categoriaRepository.save(categoria);
    }

    // Actualizar una categoria
    @Transactional
    public Categoria actualizar(
            Long id,
            String nombre,
            String descripcion,
            String color) {

        Categoria categoria = buscarPorId(id);
        String nombreLimpio = validarNombre(nombre);
        validarOtrosCampos(descripcion, color);

        categoriaRepository.findByNombreIgnoreCase(nombreLimpio)
                .ifPresent(existente -> {
                    if (!existente.getId().equals(id)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe una categoria con ese nombre"
                        );
                    }
                });

        categoria.setNombre(nombreLimpio);
        categoria.setDescripcion(descripcion);
        categoria.setColor(color);

        return categoriaRepository.save(categoria);
    }

    // Eliminar una categoria
    @Transactional
    public void eliminar(Long id) {

        Categoria categoria = buscarPorId(id);

        if (productoRepository.existsByCategoriaId(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se puede eliminar una categoria con productos"
            );
        }

        categoriaRepository.delete(categoria);
    }

    // Validar el nombre
    private String validarNombre(String nombre) {

        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la categoria es obligatorio"
            );
        }

        String nombreLimpio = nombre.trim();

        if (nombreLimpio.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar los 100 caracteres"
            );
        }

        return nombreLimpio;
    }

    // Validar descripcion y color
    private void validarOtrosCampos(
            String descripcion,
            String color) {

        if (descripcion != null && descripcion.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La descripcion no puede superar los 500 caracteres"
            );
        }

        if (color != null && color.length() > 20) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El color no puede superar los 20 caracteres"
            );
        }
    }
}
