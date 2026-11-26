
package cl.petshop.backend.config;

import cl.petshop.backend.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationManagers;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.springframework.security.authorization.AuthorityAuthorizationManager.hasRole;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final JsonMapper json;

    public SecurityConfig(JwtService jwtService, JsonMapper json) {
        this.jwtService = jwtService;
        this.json = json;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        AuthenticationEntryPoint sinSesion = (solicitud, respuesta, error) -> {
            respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            responderError(respuesta, HttpStatus.UNAUTHORIZED, "Inicia sesión para continuar");
        };
        AccessDeniedHandler sinPermiso = (solicitud, respuesta, error) ->
                responderError(respuesta, HttpStatus.FORBIDDEN, "No tienes permiso");

        http
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Registro e inicio de sesion publicos
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/registro",
                    "/api/v1/auth/login"
                ).permitAll()

                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").authenticated()

                // Perfil en la tienda y en el panel de administración.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/auth/perfil"
                ).hasAnyRole("ADMINISTRADOR", "CLIENTE")

                // El listado, los filtros y el detalle de productos son públicos.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/productos",
                    "/api/v1/productos/**"
                ).permitAll()

                // Las categorías son públicas en la tienda; no forman parte de la vista de ventas.
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/categorias",
                    "/api/v1/categorias/**"
                ).access(AuthorizationManagers.not(hasRole("VENDEDOR")))

                // Solo administradores modifican productos
                // y categorias
                .requestMatchers(
                    "/api/v1/productos",
                    "/api/v1/productos/**",
                    "/api/v1/categorias",
                    "/api/v1/categorias/**"
                ).hasRole("ADMINISTRADOR")

                // Solo administradores gestionan usuarios
                .requestMatchers(
                    "/api/v1/usuarios",
                    "/api/v1/usuarios/**"
                ).hasRole("ADMINISTRADOR")

                // Solo los clientes crean pedidos
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/pedidos"
                ).hasRole("CLIENTE")

                // Pago simulado para clientes
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/pedidos/*/pagar-simulado"
                ).hasRole("CLIENTE")

                // Administradores y vendedores consultan
                // todos los pedidos
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/pedidos"
                ).hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                // Historial del cliente autenticado
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/pedidos/mis-pedidos"
                ).hasRole("CLIENTE")

                // Consultar pedidos individuales
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/pedidos/*"
                ).hasAnyRole(
                    "CLIENTE",
                    "VENDEDOR",
                    "ADMINISTRADOR"
                )

                .requestMatchers(
                    "/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs", "/v3/api-docs/**"
                ).permitAll()
                .requestMatchers("/error").permitAll()

                .anyRequest().denyAll()
            )

            .exceptionHandling(errores -> errores
                .authenticationEntryPoint(sinSesion)
                .accessDeniedHandler(sinPermiso)
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationEntryPoint(sinSesion)
                .accessDeniedHandler(sinPermiso)
                .jwt(jwt -> jwt
                    .decoder(jwtService.obtenerDecodificador())
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                    )
                )
            )

            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    private void responderError(HttpServletResponse respuesta, HttpStatus estado, String detalle) throws IOException {
        respuesta.setStatus(estado.value());
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(respuesta.getWriter(), Map.of("type", "about:blank", "status", estado.value(),
                "title", estado.getReasonPhrase(), "detail", detalle));
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName("rol");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(roles);

        return converter;
    }
}
