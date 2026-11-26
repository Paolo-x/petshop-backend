package cl.petshop.backend;

import cl.petshop.backend.security.JwtService;
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
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:petshop_pruebas;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "petshop.jwt.duracion=PT10M"
})
@AutoConfigureMockMvc
class PetshopBackendApplicationTests {

	@Autowired
	private MockMvc mvc;
	@Autowired
	private JsonMapper json;
	@Autowired
	private JwtService jwtService;

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
	void devuelve401SinSesionY403SiElRolNoTienePermiso() throws Exception {
		mvc.perform(post(API + "/productos"))
				.andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
				.andExpect(jsonPath("$.status").value(401));
		mvc.perform(post(API + "/productos").with(rol("CLIENTE")))
				.andExpect(status().isForbidden()).andExpect(jsonPath("$.detail").value("No tienes permiso"));
		mvc.perform(get(API + "/pedidos").with(rol("CLIENTE"))).andExpect(status().isForbidden());
		mvc.perform(get(API + "/productos").with(rol("VENDEDOR"))).andExpect(status().isOk());
		mvc.perform(get(API + "/categorias")).andExpect(status().isOk());
		mvc.perform(get(API + "/categorias").with(rol("VENDEDOR"))).andExpect(status().isForbidden());
		mvc.perform(get(API + "/usuarios").with(rol("VENDEDOR"))).andExpect(status().isForbidden());
		mvc.perform(get(API + "/auth/perfil").with(rol("VENDEDOR"))).andExpect(status().isForbidden());
		mvc.perform(put(API + "/productos/1").with(rol("VENDEDOR"))).andExpect(status().isForbidden());
		mvc.perform(post(API + "/pedidos/1/pagar-simulado").with(rol("VENDEDOR"))).andExpect(status().isForbidden());
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

	@Test
	void iniciaSesionYRenuevaConUnBearerValido() throws Exception {
		String correo = "sesion@petshop.test";
		registrarCliente("90.000.011-1", correo);
		String primero = iniciarSesion(correo);
		var original = jwtService.obtenerDecodificador().decode(primero);
		assertThat(Duration.between(original.getIssuedAt(), original.getExpiresAt())).isEqualTo(Duration.ofMinutes(10));
		assertThat(original.getClaimAsString("rol")).isEqualTo("CLIENTE");

		var respuesta = mvc.perform(post(API + "/auth/refresh").header("Authorization", "Bearer " + primero))
				.andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("CLIENTE"))
				.andReturn().getResponse();
		String nuevo = json.readTree(respuesta.getContentAsString()).path("token").asString();
		assertThat(nuevo).isNotBlank().isNotEqualTo(primero);
		var renovado = jwtService.obtenerDecodificador().decode(nuevo);
		assertThat(renovado.getClaims().get("inicioSesion")).isEqualTo(original.getClaims().get("inicioSesion"));
		mvc.perform(get(API + "/auth/perfil").header("Authorization", "Bearer " + nuevo))
				.andExpect(status().isOk()).andExpect(jsonPath("$.correo").value(correo))
				.andExpect(jsonPath("$.contrasenaHash").doesNotExist());
	}

	@Test
	void rechazaClaveIncorrectaYNoRenuevaSinUnaSesionValida() throws Exception {
		String correo = "clave@petshop.test";
		registrarCliente("90.000.012-K", correo);
		mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"correo":"%s","contrasena":"ClaveIncorrecta"}
						""".formatted(correo)))
				.andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").isNotEmpty());
		mvc.perform(post(API + "/auth/refresh")).andExpect(status().isUnauthorized());
		mvc.perform(post(API + "/auth/refresh").header("Authorization", "Bearer sesion-invalida"))
				.andExpect(status().isUnauthorized());
	}

	private void registrarCliente(String run, String correo) throws Exception {
		mvc.perform(post(API + "/auth/registro").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"run":"%s","nombre":"Prueba","apellidos":"PetShop","correo":"%s","contrasena":"ClaveDemo21!"}
						""".formatted(run, correo)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value("CLIENTE"))
				.andExpect(jsonPath("$.contrasena").doesNotExist()).andExpect(jsonPath("$.contrasenaHash").doesNotExist());
	}

	private String iniciarSesion(String correo) throws Exception {
		var respuesta = mvc.perform(post(API + "/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"correo":"%s","contrasena":"ClaveDemo21!"}
						""".formatted(correo)))
				.andExpect(status().isOk()).andReturn().getResponse();
		return json.readTree(respuesta.getContentAsString()).path("token").asString();
	}
}
