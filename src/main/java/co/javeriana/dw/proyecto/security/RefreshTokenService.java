package co.javeriana.dw.proyecto.security;

import co.javeriana.dw.proyecto.entidad.RefreshToken;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.SesionInvalidaException;
import co.javeriana.dw.proyecto.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Emite, rota y revoca los tokens de larga duración (refresh tokens).
 *
 * Rotación: cada vez que se usa un refresh token se revoca y se entrega uno nuevo. Si
 * alguien roba un token y lo usa, el dueño legítimo (o el ladrón) terminará presentando
 * uno ya revocado; en ese caso se asume robo y se cierran todas las sesiones del usuario.
 *
 * Excepción a esa regla: si el token se revocó por rotación hace muy poco (GRACIA), lo más
 * probable es que dos pestañas hayan intentado renovar a la vez. Se rechaza la petición,
 * pero sin cerrar las demás sesiones.
 */
@Service
public class RefreshTokenService {

    static final Duration GRACIA = Duration.ofSeconds(30);
    private static final int BYTES_TOKEN = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final Duration duracion;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${jwt.refresh-expiration-days:7}") long diasExpiracion) {
        this(refreshTokenRepository, Duration.ofDays(diasExpiracion), Clock.systemUTC());
    }

    /** Para pruebas: permite fijar la duración y controlar el reloj. */
    RefreshTokenService(RefreshTokenRepository refreshTokenRepository, Duration duracion, Clock reloj) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.duracion = duracion;
        this.reloj = reloj;
    }

    /** Resultado de una rotación: el dueño del token y el nuevo token en claro. */
    public record Rotacion(Usuario usuario, String nuevoToken) {}

    /** Crea un refresh token nuevo y devuelve el valor en claro (es la única vez que existe). */
    @Transactional
    public String emitir(Usuario usuario) {
        Instant ahora = reloj.instant();
        refreshTokenRepository.eliminarVencidosDelUsuario(usuario.getId(), ahora);

        String tokenPlano = generarValorAleatorio();
        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(tokenPlano));
        token.setUsuario(usuario);
        token.setFechaCreacion(ahora);
        token.setFechaExpiracion(ahora.plus(duracion));
        refreshTokenRepository.save(token);
        return tokenPlano;
    }

    /**
     * Cambia un refresh token válido por uno nuevo. noRollbackFor es clave: cuando se
     * detecta reutilización hay que revocar todo Y lanzar la excepción, y la revocación
     * no puede deshacerse por culpa de esa excepción.
     */
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public Rotacion rotar(String tokenPlano) {
        Instant ahora = reloj.instant();
        RefreshToken actual = refreshTokenRepository.findByTokenHash(hash(tokenPlano))
                .orElseThrow(SesionInvalidaException::new);
        Usuario usuario = actual.getUsuario();

        if (actual.isRevocado()) {
            boolean carreraEntrePestanas = actual.isReemplazado()
                    && actual.getFechaRevocacion() != null
                    && actual.getFechaRevocacion().isAfter(ahora.minus(GRACIA));
            if (!carreraEntrePestanas) {
                refreshTokenRepository.revocarTodosDelUsuario(usuario.getId(), ahora);
            }
            throw new SesionInvalidaException();
        }
        if (actual.estaVencido(ahora)) {
            throw new SesionInvalidaException();
        }
        if (!usuario.isActivo()) {
            refreshTokenRepository.revocarTodosDelUsuario(usuario.getId(), ahora);
            throw new SesionInvalidaException();
        }

        actual.revocar(ahora, true);
        refreshTokenRepository.save(actual);
        return new Rotacion(usuario, emitir(usuario));
    }

    /** Logout. Es idempotente: un token desconocido o ya revocado no produce error. */
    @Transactional
    public void revocar(String tokenPlano) {
        refreshTokenRepository.findByTokenHash(hash(tokenPlano))
                .filter(token -> !token.isRevocado())
                .ifPresent(token -> {
                    token.revocar(reloj.instant(), false);
                    refreshTokenRepository.save(token);
                });
    }

    /** Cierra todas las sesiones del usuario, por ejemplo cuando el administrador lo desactiva. */
    @Transactional
    public void revocarTodos(Long usuarioId) {
        refreshTokenRepository.revocarTodosDelUsuario(usuarioId, reloj.instant());
    }

    public long getDuracionSegundos() {
        return duracion.toSeconds();
    }

    private String generarValorAleatorio() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String valor) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM", e);
        }
    }
}
