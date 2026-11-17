
package cl.petshop.backend.service;

import cl.petshop.backend.model.Categoria;
import cl.petshop.backend.repository.CategoriaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
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
    public Categoria crear(String nombre, String imagen) {

        validarDatos(nombre, imagen);

        String nombreLimpio = nombre.trim();

        if (categoriaRepository.existsByNombreIgnoreCase(nombreLimpio)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una categoria con ese nombre"
            );
        }

        Categoria categoria = new Categoria();

        categoria.setNombre(nombreLimpio);
        categoria.setImagen(limpiarImagen(imagen));

        return categoriaRepository.save(categoria);
    }

    // Actualizar una categoria
    @Transactional
    public Categoria actualizar(Long id, String nombre, String imagen) {

        Categoria categoria = buscarPorId(id);

        validarDatos(nombre, imagen);

        String nombreLimpio = nombre.trim();

        categoriaRepository.findByNombreIgnoreCase(nombreLimpio)
                .ifPresent(existente -> {

                    if (!existente.getId().equals(id)) {
                        throw new ResponseStatusException(
                                HttpStatus.CONFLICT,
                                "Ya existe otra categoria con ese nombre"
                        );
                    }
                });

        categoria.setNombre(nombreLimpio);
        categoria.setImagen(limpiarImagen(imagen));

        return categoriaRepository.save(categoria);
    }

    // Eliminar una categoria
    @Transactional
    public void eliminar(Long id) {

        Categoria categoria = buscarPorId(id);

        categoriaRepository.delete(categoria);
    }

    // Validar los datos de una categoria
    private void validarDatos(String nombre, String imagen) {

        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la categoria es obligatorio"
            );
        }

        if (nombre.trim().length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre no puede superar los 100 caracteres"
            );
        }

        if (imagen != null && imagen.trim().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La imagen no puede superar los 500 caracteres"
            );
        }
    }

    // Limpiar la ruta de la imagen
    private String limpiarImagen(String imagen) {

        if (imagen == null || imagen.isBlank()) {
            return null;
        }

        return imagen.trim();
    }
}
