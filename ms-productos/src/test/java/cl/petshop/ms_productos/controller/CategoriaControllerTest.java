
package cl.petshop.ms_productos.controller;

import cl.petshop.ms_productos.model.Categoria;
import cl.petshop.ms_productos.service.CategoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CategoriaControllerTest {

    @Mock
    private CategoriaService categoriaService;

    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {

        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();

        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new CategoriaController(categoriaService)
                )
                .setValidator(validator)
                .build();
    }

    // Crear una categoria de ejemplo
    private Categoria categoriaEjemplo() {

        Categoria categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNombre("Alimentos");
        categoria.setDescripcion("Comida para mascotas");
        categoria.setColor("#3498db");

        return categoria;
    }

    // Prueba 1: Listar categorias
    @Test
    void listarCategoriasCorrectamente() throws Exception {

        when(categoriaService.listar())
                .thenReturn(List.of(categoriaEjemplo()));

        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Alimentos"));
    }

    // Prueba 2: Buscar una categoria por ID
    @Test
    void buscarCategoriaPorId() throws Exception {

        when(categoriaService.buscarPorId(1L))
                .thenReturn(categoriaEjemplo());

        mockMvc.perform(get("/api/v1/categorias/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Alimentos"));
    }

    // Prueba 3: Crear una categoria
    @Test
    void crearCategoriaCorrectamente() throws Exception {

        when(categoriaService.crear(
                "Alimentos",
                "Comida para mascotas",
                "#3498db"
        )).thenReturn(categoriaEjemplo());

        String json = """
                {
                  "nombre": "Alimentos",
                  "descripcion": "Comida para mascotas",
                  "color": "#3498db"
                }
                """;

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Alimentos"));

        verify(categoriaService).crear(
                "Alimentos",
                "Comida para mascotas",
                "#3498db"
        );
    }

    // Prueba 4: Rechazar una categoria sin nombre
    @Test
    void rechazarCategoriaSinNombre() throws Exception {

        String json = """
                {
                  "nombre": "",
                  "descripcion": "Sin nombre",
                  "color": "#3498db"
                }
                """;

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(categoriaService);
    }
}
