
package cl.petshop.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            // Configuracion temporal para probar la API REST
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/v1/**")
            )

            // Exigir autenticacion
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )

            // Autenticacion Basic temporal
            .httpBasic(Customizer.withDefaults())

            // No mantener sesiones de autenticacion
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );

        return http.build();
    }
}
