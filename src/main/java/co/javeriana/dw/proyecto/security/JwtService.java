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
 * Crea y valida el token de corta duración (access token). Un JWT son tres partes
 * (cabecera.datos.firma): los datos (claims) van en claro, y la firma HMAC con nuestra
 * clave secreta garantiza que nadie los modificó. Por eso NO se guarda nada sensible
 * (contraseñas) dentro del token.
 *
 * El access token dura pocos minutos porque no se puede revocar: una vez emitido es
 * válido hasta que vence. Para no obligar al usuario a iniciar sesión cada 15 minutos,
 * existe el refresh token (ver RefreshTokenService), que sí vive en la base de datos.
 */
@Service
public class JwtService {

    /** Claim que distingue el access token de cualquier otro JWT firmado con la misma clave. */
    static final String CLAIM_TIPO = "tipo";
    static final String TIPO_ACCESO = "access";

    private final SecretKey key;
    private final long expiracionMinutos;

    public JwtService(@Value("${jwt.secret}") String secretBase64,
                      @Value("${jwt.access-expiration-minutes:15}") long expiracionMinutos) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretBase64));
        this.expiracionMinutos = expiracionMinutos;
    }

    public String generarAccessToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim(CLAIM_TIPO, TIPO_ACCESO)
                .claim("usuarioId", usuario.getId())
                .claim("empresaId", usuario.getEmpresa().getId())
                .claim("rol", usuario.getRolUsuario().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(expiracionMinutos, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Segundos de vida del access token; el frontend lo recibe como expiresIn. */
    public long getExpiracionSegundos() {
        return expiracionMinutos * 60;
    }

    /**
     * Devuelve el usuario si el token es un access token válido; vacío si está alterado,
     * mal formado, vencido o no es de tipo "access".
     */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(token).getPayload();
            if (!TIPO_ACCESO.equals(claims.get(CLAIM_TIPO, String.class))) {
                return Optional.empty();
            }
            return Optional.of(new UsuarioAutenticado(
                    ((Number) claims.get("usuarioId")).longValue(),
                    ((Number) claims.get("empresaId")).longValue(),
                    claims.getSubject(),
                    RolUsuario.valueOf(claims.get("rol", String.class))));
        } catch (JwtException | IllegalArgumentException | NullPointerException | ClassCastException e) {
            return Optional.empty();
        }
    }
}
