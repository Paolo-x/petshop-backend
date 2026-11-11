
package cl.petshop.ms_productos.service;

import cl.petshop.ms_productos.dto.ProductoRequest;
import cl.petshop.ms_productos.model.Categoria;
import cl.petshop.ms_productos.model.Producto;
import cl.petshop.ms_productos.repository.CategoriaRepository;
import cl.petshop.ms_productos.repository.ProductoRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProductoService productoService;

    // Datos de ejemplo para las pruebas
    private ProductoRequest productoValido() {

        return new ProductoRequest(
                "P001",
                "Alimento para perros",
                "Bolsa de alimento de 10 kg",
                new BigDecimal("25000"),
                new BigDecimal("19990"),
                20,
                5,
                null,
                1L
        );
    }

    // Prueba 1: Registrar un producto correctamente
    @Test
    void crearProductoCorrectamente() {

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Alimentos");

        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(categoria));

        when(productoRepository.save(any(Producto.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        Producto resultado = productoService.crear(productoValido());

        assertNotNull(resultado);
        assertEquals("P001", resultado.getCodigo());
        assertEquals("Alimento para perros", resultado.getNombre());
        assertEquals(20, resultado.getStock());
        assertEquals(1L, resultado.getCategoria().getId());

        verify(productoRepository).save(any(Producto.class));
    }

    // Prueba 2: Rechazar un codigo duplicado
    @Test
    void rechazarCodigoDuplicado() {

        when(productoRepository.existsByCodigoIgnoreCase("P001"))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> productoService.crear(productoValido())
        );

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());

        verify(productoRepository, never())
                .save(any(Producto.class));
    }

    // Prueba 3: Rechazar un precio de oferta invalido
    @Test
    void rechazarPrecioOfertaInvalido() {

        ProductoRequest request = new ProductoRequest(
                "P001",
                "Alimento para perros",
                "Bolsa de alimento",
                new BigDecimal("25000"),
                new BigDecimal("30000"),
                20,
                5,
                null,
                1L
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> productoService.crear(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verify(productoRepository, never())
                .save(any(Producto.class));
    }

    // Prueba 4: Rechazar stock negativo
    @Test
    void rechazarStockNegativo() {

        ProductoRequest request = new ProductoRequest(
                "P001",
                "Alimento para perros",
                "Bolsa de alimento",
                new BigDecimal("25000"),
                null,
                -5,
                5,
                null,
                1L
        );

        ResponseStatusException error = assertThrows(
                ResponseStatusException.class,
                () -> productoService.crear(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());

        verify(productoRepository, never())
                .save(any(Producto.class));
    }
}
