
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
            // La API usara JWT, no sesiones tradicionales
            .csrf(AbstractHttpConfigurer::disable)

            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )

            // Configuracion temporal de permisos
            .authorizeHttpRequests(auth -> auth

                // Consultar categorias es publico
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/v1/categorias",
                    "/api/v1/categorias/*"
                ).permitAll()

                // Bloquear las demas operaciones
                // hasta implementar JWT
                .anyRequest().denyAll()
            )

            // No utilizaremos login tradicional
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
