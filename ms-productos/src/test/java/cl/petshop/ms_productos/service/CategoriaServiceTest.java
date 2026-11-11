
package cl.petshop.ms_productos.service;

import cl.petshop.ms_productos.model.Categoria;
import cl.petshop.ms_productos.repository.CategoriaRepository;
import cl.petshop.ms_productos.repository.ProductoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private CategoriaService categoriaService;

    // Prueba 1: Crear una categoria correctamente
    @Test
    void crearCategoriaCorrectamente() {

        when(categoriaRepository.existsByNombreIgnoreCase("Alimentos"))
                .thenReturn(false);

        when(categoriaRepository.save(any(Categoria.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Categoria resultado = categoriaService.crear(
                "Alimentos",
                "Comida para mascotas",
                "#3498db"
        );

        assertNotNull(resultado);
        assertEquals("Alimentos", resultado.getNombre());
        assertEquals("Comida para mascotas", resultado.getDescripcion());

        verify(categoriaRepository).save(any(Categoria.class));
    }

    // Prueba 2: Rechazar un nombre duplicado
    @Test
    void rechazarCategoriaDuplicada() {

        when(categoriaRepository.existsByNombreIgnoreCase("Alimentos"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> categoriaService.crear(
                        "Alimentos",
                        "Otra descripcion",
                        "#FFFFFF"
                )
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(categoriaRepository, never())
                .save(any(Categoria.class));
    }

    // Prueba 3: No eliminar una categoria con productos
    @Test
    void impedirEliminarCategoriaConProductos() {

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Alimentos");

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(productoRepository.existsByCategoriaId(1L))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> categoriaService.eliminar(1L)
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(categoriaRepository, never())
                .delete(any(Categoria.class));
    }
}
