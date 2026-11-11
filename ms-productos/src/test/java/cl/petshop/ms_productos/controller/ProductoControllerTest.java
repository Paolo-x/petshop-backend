
package cl.petshop.ms_productos.controller;

import cl.petshop.ms_productos.model.Categoria;
import cl.petshop.ms_productos.model.Producto;
import cl.petshop.ms_productos.service.ProductoService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProductoControllerTest {

    @Mock
    private ProductoService productoService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ProductoController(productoService)
                )
                .setValidator(validator)
                .build();
    }

    // Crear producto de ejemplo
    private Producto productoEjemplo() {

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Alimentos");

        Producto producto = new Producto();

        producto.setId(1L);
        producto.setCodigo("P001");
        producto.setNombre("Alimento para perros");
        producto.setDescripcion("Bolsa de alimento de 10 kg");
        producto.setPrecio(new BigDecimal("25000"));
        producto.setPrecioOferta(new BigDecimal("19990"));
        producto.setStock(20);
        producto.setStockCritico(5);
        producto.setCategoria(categoria);

        return producto;
    }

    // Prueba 1: Listar productos
    @Test
    void listarProductosCorrectamente() throws Exception {

        when(productoService.buscarPorNombre(null))
                .thenReturn(List.of(productoEjemplo()));

        mockMvc.perform(get("/api/v1/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("P001"))
                .andExpect(jsonPath("$[0].nombre")
                        .value("Alimento para perros"))
                .andExpect(jsonPath("$[0].categoriaNombre")
                        .value("Alimentos"));
    }

    // Prueba 2: Filtrar por categoria y nombre
    @Test
    void filtrarProductosCorrectamente() throws Exception {

        when(productoService.buscarPorCategoriaYNombre(
                1L, "perro"
        )).thenReturn(List.of(productoEjemplo()));

        mockMvc.perform(get("/api/v1/productos")
                        .param("categoriaId", "1")
                        .param("nombre", "perro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("P001"));

        verify(productoService)
                .buscarPorCategoriaYNombre(1L, "perro");
    }

    // Prueba 3: Crear un producto valido
    @Test
    void crearProductoCorrectamente() throws Exception {

        when(productoService.crear(any()))
                .thenReturn(productoEjemplo());

        String json = """
                {
                  "codigo": "P001",
                  "nombre": "Alimento para perros",
                  "descripcion": "Bolsa de alimento de 10 kg",
                  "precio": 25000,
                  "precioOferta": 19990,
                  "stock": 20,
                  "stockCritico": 5,
                  "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("P001"))
                .andExpect(jsonPath("$.categoriaNombre")
                        .value("Alimentos"));

        verify(productoService).crear(any());
    }

    // Prueba 4: Rechazar stock negativo
    @Test
    void rechazarProductoConStockNegativo() throws Exception {

        String json = """
                {
                  "codigo": "P001",
                  "nombre": "Alimento para perros",
                  "precio": 25000,
                  "stock": -5,
                  "stockCritico": 5,
                  "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/productos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productoService);
    }

    // Prueba 5: Eliminar un producto
    @Test
    void eliminarProductoCorrectamente() throws Exception {

        mockMvc.perform(delete("/api/v1/productos/1"))
                .andExpect(status().isNoContent());

        verify(productoService).eliminar(1L);
    }
}
