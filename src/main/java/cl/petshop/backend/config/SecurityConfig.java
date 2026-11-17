
package cl.petshop.backend.config;

import cl.petshop.backend.security.JwtService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // API sin sesiones, autenticada mediante JWT
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Permisos de acceso
            .authorizeHttpRequests(auth -> auth

                // Registro e inicio de sesion publicos
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/auth/registro",
                    "/api/v1/auth/login"
                ).permitAll()

                // Consultar productos y categorias
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/productos",
                    "/api/v1/productos/**",
                    "/api/v1/categorias",
                    "/api/v1/categorias/**"
                ).permitAll()

                // Solo administradores modifican productos y categorias
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

                // Los clientes pueden crear pedidos
                .requestMatchers(
                    HttpMethod.POST,
                    "/api/v1/pedidos"
                ).hasRole("CLIENTE")

                // Administradores y vendedores consultan todos los pedidos
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/pedidos"
                ).hasAnyRole("ADMINISTRADOR", "VENDEDOR")

                // Los clientes consultan su historial de compras
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/pedidos/mis-pedidos"
                ).hasRole("CLIENTE")

                // Consultar un pedido individual
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/pedidos/*"
                ).hasAnyRole(
                    "CLIENTE",
                    "VENDEDOR",
                    "ADMINISTRADOR"
                )

                // Permitir respuestas de error
                .requestMatchers("/error").permitAll()

                // Bloquear las rutas no configuradas
                .anyRequest().denyAll()
            )

            // Validar los tokens JWT
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtService.obtenerDecodificador())
                    .jwtAuthenticationConverter(
                        jwtAuthenticationConverter()
                    )
                )
            )

            // Desactivar autenticacion Basic
            .httpBasic(AbstractHttpConfigurer::disable)

            // Desactivar formulario de login predeterminado
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }

    // Convertir el rol del JWT en un permiso de Spring Security
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
