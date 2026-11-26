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
	void admiteSolicitudesDelOrigenConfiguradoEnElNavegador() throws Exception {
		mvc.perform(options(API + "/productos").header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "authorization,content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
		mvc.perform(options(API + "/productos").header("Origin", "http://localhost:5174")
				.header("Access-Control-Request-Method", "POST"))
				.andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
	}

	@Test
	void swaggerDescribeTodasLasOperacionesYSusPermisos() throws Exception {
		mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
		var respuesta = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andReturn().getResponse();
		var rutas = json.readTree(respuesta.getContentAsString()).path("paths");
		assertThat(rutas.size()).isEqualTo(16);
		int operaciones = 0;
		for (var ruta : rutas) {
			for (var operacion : ruta) {
				assertThat(operacion.path("summary").asString()).isNotBlank();
				assertThat(operacion.path("responses").size()).isPositive();
				operaciones++;
			}
		}
		assertThat(operaciones).isEqualTo(26);
		assertThat(rutas.path(API + "/auth/login").path("post").has("security")).isFalse();
		assertThat(rutas.path(API + "/auth/refresh").path("post").path("security").get(0).has("bearerAuth")).isTrue();
		assertThat(rutas.path(API + "/pedidos/{id}/pagar-simulado").path("post").path("responses").has("409")).isTrue();
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

	@Test
	void descuentaStockSoloAlPagarYPermiteLaConsultaDelVendedor() throws Exception {
		String correo = "compras@petshop.test";
		registrarCliente("90.000.013-8", correo);
		String cliente = iniciarSesion(correo);
		long productoId = crearProducto("PAGO-OK", 2500, 7);
		long pedidoId = crearPedido(cliente, productoId, 2);
		verificarStock(productoId, 7);

		mvc.perform(post(API + "/pedidos/{id}/pagar-simulado", pedidoId).header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("pagada"))
				.andExpect(jsonPath("$.total").value(5000)).andExpect(jsonPath("$.lineas[0].cantidad").value(2));
		verificarStock(productoId, 5);
		mvc.perform(post(API + "/pedidos/{id}/pagar-simulado", pedidoId).header("Authorization", "Bearer " + cliente))
				.andExpect(status().isConflict());
		verificarStock(productoId, 5);

		mvc.perform(get(API + "/pedidos/mis-pedidos").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(pedidoId));
		String correoVendedor = "pedidos-vendedor@petshop.test";
		crearUsuario("90.000.014-6", correoVendedor, "VENDEDOR");
		String vendedor = iniciarSesion(correoVendedor);
		mvc.perform(get(API + "/pedidos").header("Authorization", "Bearer " + vendedor)).andExpect(status().isOk());
		mvc.perform(get(API + "/pedidos/{id}", pedidoId).header("Authorization", "Bearer " + vendedor))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(pedidoId));
		mvc.perform(delete(API + "/productos/{id}", productoId).with(rol("ADMINISTRADOR")))
				.andExpect(status().isConflict());
	}

	@Test
	void rechazaComprasSinStockYNoMuestraPedidosAOtrosClientes() throws Exception {
		String correo = "sin-stock@petshop.test";
		registrarCliente("90.000.015-4", correo);
		String cliente = iniciarSesion(correo);
		long productoId = crearProducto("STOCK-LIMITADO", 9000, 1);
		mvc.perform(post(API + "/pedidos").header("Authorization", "Bearer " + cliente)
				.contentType(MediaType.APPLICATION_JSON).content(datosPedido(productoId, 2)))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.detail").isNotEmpty());
		mvc.perform(get(API + "/pedidos/mis-pedidos").header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
		long primerPedido = crearPedido(cliente, productoId, 1);
		long segundoPedido = crearPedido(cliente, productoId, 1);

		String correoAjeno = "pedido-ajeno@petshop.test";
		registrarCliente("90.000.016-2", correoAjeno);
		String ajeno = iniciarSesion(correoAjeno);
		mvc.perform(get(API + "/pedidos/{id}", primerPedido).header("Authorization", "Bearer " + ajeno))
				.andExpect(status().isForbidden());
		mvc.perform(post(API + "/pedidos/{id}/pagar-simulado", primerPedido).header("Authorization", "Bearer " + ajeno))
				.andExpect(status().isForbidden());

		mvc.perform(post(API + "/pedidos/{id}/pagar-simulado", primerPedido).header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk());
		mvc.perform(post(API + "/pedidos/{id}/pagar-simulado", segundoPedido).header("Authorization", "Bearer " + cliente))
				.andExpect(status().isConflict());
		verificarStock(productoId, 0);
	}

	@Test
	void conservaElStockCuandoLaPasarelaRechazaElMonto() throws Exception {
		String correo = "pago-fallido@petshop.test";
		registrarCliente("90.000.017-0", correo);
		String cliente = iniciarSesion(correo);
		long productoId = crearProducto("PAGO-FALLIDO", 110000, 3);
		long pedidoId = crearPedido(cliente, productoId, 1);
		mvc.perform(post(API + "/pedidos/{id}/pagar-simulado", pedidoId).header("Authorization", "Bearer " + cliente))
				.andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("fallida"));
		verificarStock(productoId, 3);
	}

	@Test
	void administraUsuariosSinExponerLaClave() throws Exception {
		String correo = "usuario-admin@petshop.test";
		long id = crearUsuario("90.000.018-9", correo, "VENDEDOR");
		mvc.perform(get(API + "/usuarios/{id}", id).with(rol("ADMINISTRADOR")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.correo").value(correo))
				.andExpect(jsonPath("$.contrasenaHash").doesNotExist());
		String token = iniciarSesion(correo);
		mvc.perform(put(API + "/usuarios/{id}", id).with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"run":"90.000.018-9","nombre":"Nueva","apellidos":"Cuenta","correo":"%s","rol":"CLIENTE"}
						""".formatted(correo)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Nueva"));
		mvc.perform(post(API + "/auth/refresh").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.rol").value("CLIENTE"));
		mvc.perform(delete(API + "/usuarios/{id}", id).with(rol("ADMINISTRADOR"))).andExpect(status().isNoContent());
		mvc.perform(get(API + "/usuarios/{id}", id).with(rol("ADMINISTRADOR"))).andExpect(status().isNotFound());
		mvc.perform(post(API + "/auth/refresh").header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	private long crearProducto(String codigo, int precio, int stock) throws Exception {
		var categoria = mvc.perform(post(API + "/categorias").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"nombre":"Categoría %s"}
						""".formatted(codigo)))
				.andExpect(status().isCreated()).andReturn().getResponse();
		long categoriaId = json.readTree(categoria.getContentAsString()).path("id").asLong();
		var producto = mvc.perform(post(API + "/productos").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"codigo":"%s","nombre":"Producto de prueba","categoria":{"id":%d},"precio":%d,"stock":%d}
						""".formatted(codigo, categoriaId, precio, stock)))
				.andExpect(status().isCreated()).andReturn().getResponse();
		return json.readTree(producto.getContentAsString()).path("id").asLong();
	}

	private long crearUsuario(String run, String correo, String rol) throws Exception {
		var respuesta = mvc.perform(post(API + "/usuarios").with(rol("ADMINISTRADOR"))
				.contentType(MediaType.APPLICATION_JSON).content("""
						{"run":"%s","nombre":"Prueba","apellidos":"PetShop","correo":"%s","contrasena":"ClaveDemo21!","rol":"%s"}
						""".formatted(run, correo, rol)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.rol").value(rol))
				.andExpect(jsonPath("$.contrasenaHash").doesNotExist()).andReturn().getResponse();
		return json.readTree(respuesta.getContentAsString()).path("id").asLong();
	}

	private long crearPedido(String token, long productoId, int cantidad) throws Exception {
		var pedido = mvc.perform(post(API + "/pedidos").header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON).content(datosPedido(productoId, cantidad)))
				.andExpect(status().isCreated()).andExpect(jsonPath("$.estado").value("pendiente"))
				.andReturn().getResponse();
		return json.readTree(pedido.getContentAsString()).path("id").asLong();
	}

	private String datosPedido(long productoId, int cantidad) {
		return """
				{"nombreEntrega":"Persona","apellidosEntrega":"De prueba","correoEntrega":"entrega@petshop.test",
				 "calle":"Calle de prueba 123","region":"Metropolitana","comuna":"Santiago",
				 "lineas":[{"productoId":%d,"cantidad":%d}]}
				""".formatted(productoId, cantidad);
	}

	private void verificarStock(long productoId, int stock) throws Exception {
		mvc.perform(get(API + "/productos/{id}", productoId)).andExpect(status().isOk())
				.andExpect(jsonPath("$.stock").value(stock));
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
