
package cl.petshop.backend.security;

import cl.petshop.backend.model.Usuario;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public JwtService(@Value("${JWT_SECRET}") String secreto) {

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

        this.jwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    // Generar token para un usuario
    public String generarToken(Usuario usuario) {

        Instant ahora = Instant.now();

        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("petshop-backend")
                .issuedAt(ahora)
                .expiresAt(ahora.plus(1, ChronoUnit.HOURS))
                .subject(usuario.getCorreo())
                .claim("id", usuario.getId())
                .claim("rol", usuario.getRol().name())
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
