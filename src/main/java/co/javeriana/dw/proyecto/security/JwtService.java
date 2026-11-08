package co.javeriana.dw.proyecto.security;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;

/**
 * Crea y valida tokens JWT. Un JWT son tres partes (cabecera.datos.firma): los datos
 * (claims) van en claro, y la firma HMAC con nuestra clave secreta garantiza que nadie
 * los modificó. Por eso NO se guarda nada sensible (contraseñas) dentro del token.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expiracionMinutos;

    public JwtService(@Value("${jwt.secret}") String secretBase64,
                      @Value("${jwt.expiration-minutes:120}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("usuarioId", usuario.getId())
                .claim("empresaId", usuario.getEmpresa().getId())
                .claim("rol", usuario.getRolUsuario().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Devuelve el usuario si el token es válido; vacío si está alterado, mal formado o vencido. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(new UsuarioAutenticado(
                    ((Number) claims.get("usuarioId")).longValue(),
                    ((Number) claims.get("empresaId")).longValue(),
                    claims.getSubject(),
                    RolUsuario.valueOf(claims.get("rol", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return Optional.empty();
        }
    }
}
