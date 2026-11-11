
package cl.petshop.ms_productos.exception;

import cl.petshop.ms_productos.controller.CategoriaController;
import cl.petshop.ms_productos.service.CategoriaService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

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
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .setValidator(validator)
                .build();
    }

    // Prueba 1: Categoria inexistente
    @Test
    void responder404CuandoCategoriaNoExiste() throws Exception {

        when(categoriaService.buscarPorId(999L))
                .thenThrow(new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "La categoria no existe"
                ));

        mockMvc.perform(get("/api/v1/categorias/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.estado").value(404))
                .andExpect(jsonPath("$.mensaje")
                        .value("La categoria no existe"))
                .andExpect(jsonPath("$.ruta")
                        .value("/api/v1/categorias/999"))
                .andExpect(jsonPath("$.fecha").exists());
    }

    // Prueba 2: Categoria duplicada
    @Test
    void responder409CuandoCategoriaEstaDuplicada() throws Exception {

        when(categoriaService.crear(
                "Alimentos",
                "Comida para mascotas",
                "#3498db"
        )).thenThrow(new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Ya existe una categoria con ese nombre"
        ));

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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.estado").value(409))
                .andExpect(jsonPath("$.mensaje")
                        .value("Ya existe una categoria con ese nombre"));
    }

    // Prueba 3: Datos invalidos
    @Test
    void responder400CuandoNombreEstaVacio() throws Exception {

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje")
                        .value("Los datos ingresados no son validos"))
                .andExpect(jsonPath("$.detalles[0]").exists());

        verifyNoInteractions(categoriaService);
    }

    // Prueba 4: JSON incorrecto
    @Test
    void responder400CuandoJsonEsInvalido() throws Exception {

        String json = """
                {
                  "nombre":
                """;

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.mensaje")
                        .value("El cuerpo JSON de la solicitud no es valido"));

        verifyNoInteractions(categoriaService);
    }
}
