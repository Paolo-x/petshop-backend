
package cl.petshop.backend.security;

import cl.petshop.backend.model.Usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.time.Instant;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@Service
public class JwtService {

    private static final String EMISOR = "petshop-backend";
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final Duration duracion;
    private final Duration duracionMaxima;

    public JwtService(@Value("${JWT_SECRET}") String secreto,
                      @Value("${petshop.jwt.duracion}") String duracion,
                      @Value("${petshop.jwt.duracion-maxima}") String duracionMaxima) {

        this.duracion = Duration.parse(duracion);
        this.duracionMaxima = Duration.parse(duracionMaxima);
        if (this.duracion.getSeconds() < 1 || this.duracionMaxima.compareTo(this.duracion) < 0) {
            throw new IllegalArgumentException("La duración del JWT debe ser positiva y no superar la sesión máxima");
        }

        byte[] clave;

        try {
            clave = Base64.getDecoder().decode(secreto);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "JWT_SECRET debe estar en formato Base64",
                    e
            );
        }

        if (clave.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET debe contener al menos 32 bytes"
            );
        }

        SecretKey secretKey = new SecretKeySpec(
                clave,
                "HmacSHA256"
        );

        this.jwtEncoder = NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator fechas = new JwtTimestampValidator(Duration.ZERO);
        fechas.setAllowEmptyExpiryClaim(false);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(fechas, new JwtIssuerValidator(EMISOR)));
        this.jwtDecoder = decoder;
    }

    // Generar token para un usuario
    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        return firmar(usuario, ahora, ahora);
    }

    // Un JWT válido permite seguir con la sesión, pero no reinicia el límite total.
    public String renovarToken(Usuario usuario, Jwt anterior) {
        Number inicio = anterior.getClaim("inicioSesion");
        Instant ahora = Instant.now();
        if (inicio == null || !ahora.isBefore(anterior.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Vuelve a iniciar sesión");
        }
        Instant fechaInicio = Instant.ofEpochSecond(inicio.longValue());
        if (!ahora.isBefore(fechaInicio.plus(duracionMaxima))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La sesión alcanzó su duración máxima");
        }
        return firmar(usuario, ahora.truncatedTo(ChronoUnit.SECONDS), fechaInicio);
    }

    private String firmar(Usuario usuario, Instant ahora, Instant inicioSesion) {
        Instant vence = ahora.plus(duracion);
        Instant limite = inicioSesion.plus(duracionMaxima);

        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer(EMISOR)
                .issuedAt(ahora)
                .expiresAt(vence.isBefore(limite) ? vence : limite)
                .id(UUID.randomUUID().toString())
                .subject(usuario.getCorreo())
                .claim("id", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .claim("inicioSesion", inicioSesion.getEpochSecond())
                .build();

        JwsHeader encabezado = JwsHeader
                .with(MacAlgorithm.HS256)
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(encabezado, datos)
        ).getTokenValue();
    }

    // Obtener decodificador para validar tokens
    public JwtDecoder obtenerDecodificador() {
        return jwtDecoder;
    }
}
