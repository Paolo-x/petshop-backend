
package cl.petshop.ms_productos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            .authorizeHttpRequests(auth -> auth

                // Documentacion Swagger publica
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**"
                ).permitAll()

                // Stock critico protegido
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/productos/stock-critico"
                ).denyAll()

                // Consultas publicas de categorias
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/categorias",
                    "/api/v1/categorias/*"
                ).permitAll()

                // Consultas publicas de productos
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/productos",
                    "/api/v1/productos/*",
                    "/api/v1/productos/codigo/*"
                ).permitAll()

                // Operaciones administrativas bloqueadas
                // hasta implementar JWT
                .anyRequest().denyAll()
            )

            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
