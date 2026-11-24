
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

                // Perfil del usuario autenticado
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/auth/perfil"
                ).authenticated()

                // Consultar productos y categorias
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/productos",
                    "/api/v1/productos/**",
                    "/api/v1/categorias",
                    "/api/v1/categorias/**"
                ).permitAll()

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

                .requestMatchers("/error").permitAll()

                .anyRequest().denyAll()
            )

            .oauth2ResourceServer(oauth2 -> oauth2
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
