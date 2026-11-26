package cl.petshop.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petshop_pruebas;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class PetshopBackendApplicationTests {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JsonMapper json;

	private static final String API = "/api/v1";

	@DynamicPropertySource
	static void prepararClaveDePrueba(DynamicPropertyRegistry propiedades) {
		byte[] clave = new byte[48];
		new SecureRandom().nextBytes(clave);
		propiedades.add("JWT_SECRET", () -> Base64.getEncoder().encodeToString(clave));
	}

	@Test
	void iniciaLaApiConBaseEnMemoria() throws Exception {
		mvc.perform(get(API + "/productos")).andExpect(status().isOk());
	}

	@Test
	void administraProductosYCategoriasSinAceptarDatosIncorrectos() throws Exception {
		mvc.perform(post(API + "/categorias").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos[0].campo").value("nombre"));
		var categoria = mvc.perform(post(API + "/categorias").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"nombre":"Categoría de catálogo"}
						"""))
				.andExpect(status().isCreated()).andReturn().getResponse();
		long categoriaId = json.readTree(categoria.getContentAsString()).path("id").asLong();
		mvc.perform(put(API + "/categorias/{id}", categoriaId).with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"nombre":"Categoría actualizada"}
						"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Categoría actualizada"));

		String producto = """
				{"codigo":"EP3-PRUEBA","nombre":"Correa","categoria":{"id":%d},"precio":3500,"stock":8}
				""".formatted(categoriaId);
		mvc.perform(post(API + "/productos").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content(producto.replace("3500", "-5")))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos[0].campo").value("precio"));
		var creado = mvc.perform(post(API + "/productos").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content(producto))
				.andExpect(status().isCreated()).andReturn().getResponse();
		long id = json.readTree(creado.getContentAsString()).path("id").asLong();
		mvc.perform(get(API + "/productos/{id}", id)).andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(8));
		mvc.perform(put(API + "/productos/{id}", id).with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content(producto.replace("Correa", "Correa nueva")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Correa nueva"));
		mvc.perform(delete(API + "/categorias/{id}", categoriaId).with(rol("ADMINISTRADOR")))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.detail").isNotEmpty());
		mvc.perform(delete(API + "/productos/{id}", id).with(rol("ADMINISTRADOR"))).andExpect(status().isNoContent());
		mvc.perform(get(API + "/productos/{id}", id)).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
		mvc.perform(delete(API + "/categorias/{id}", categoriaId).with(rol("ADMINISTRADOR")))
				.andExpect(status().isNoContent());
		mvc.perform(get(API + "/categorias/{id}", categoriaId)).andExpect(status().isNotFound());
	}

	private JwtRequestPostProcessor rol(String rol) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
	}
}
