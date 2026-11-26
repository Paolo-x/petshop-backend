package cl.petshop.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

	@DynamicPropertySource
	static void prepararClaveDePrueba(DynamicPropertyRegistry propiedades) {
		byte[] clave = new byte[48];
		new SecureRandom().nextBytes(clave);
		propiedades.add("JWT_SECRET", () -> Base64.getEncoder().encodeToString(clave));
	}

	@Test
	void iniciaLaApiConBaseEnMemoria() throws Exception {
		mvc.perform(get("/api/v1/productos")).andExpect(status().isOk());
	}
}
