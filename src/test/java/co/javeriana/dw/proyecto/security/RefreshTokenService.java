package co.javeriana.dw.proyecto.security;

import co.javeriana.dw.proyecto.entidad.RefreshToken;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.SesionInvalidaException;
import co.javeriana.dw.proyecto.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Reglas del token de larga duración (HU-03): se guarda con hash, se rota en cada uso,
 * vence, se revoca y detecta reutilización. El repositorio está simulado con un mapa.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration SIETE_DIAS = Duration.ofDays(7);

    @Mock
    private RefreshTokenRepository repository;

    private final Map<String, RefreshToken> tabla = new HashMap<>();
    private RelojAjustable reloj;
    private RefreshTokenService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        reloj = new RelojAjustable(Instant.parse("2026-11-19T12:00:00Z"));
        service = new RefreshTokenService(repository, SIETE_DIAS, reloj);

        usuario = new Usuario();
        usuario.setId(3L);
        usuario.setActivo(true);

        lenient().when(repository.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken token = inv.getArgument(0);
            tabla.put(token.getTokenHash(), token);
            return token;
        });
        lenient().when(repository.findByTokenHash(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(tabla.get(inv.<String>getArgument(0))));
    }

    @Test
    @DisplayName("Se guarda el hash del token, nunca el valor en claro")
    void emitir_guardaSoloElHash() {
        String token = service.emitir(usuario);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(repository).save(captor.capture());
        RefreshToken guardado = captor.getValue();

        assertThat(guardado.getTokenHash()).isNotEqualTo(token).hasSize(64);
        assertThat(guardado.getTokenHash()).isEqualTo(RefreshTokenService.hash(token));
        assertThat(guardado.getFechaExpiracion()).isEqualTo(reloj.instant().plus(SIETE_DIAS));
        assertThat(guardado.isRevocado()).isFalse();
    }

    @Test
    @DisplayName("Dos emisiones producen tokens distintos")
    void emitir_generaValoresDistintos() {
        assertThat(service.emitir(usuario)).isNotEqualTo(service.emitir(usuario));
    }

    @Test
    @DisplayName("Rotar entrega un token nuevo y deja inservible el anterior")
    void rotar_revocaElAnteriorYEmiteUnoNuevo() {
        String original = service.emitir(usuario);

        RefreshTokenService.Rotacion rotacion = service.rotar(original);

        assertThat(rotacion.usuario()).isSameAs(usuario);
        assertThat(rotacion.nuevoToken()).isNotEqualTo(original);
        RefreshToken anterior = tabla.get(RefreshTokenService.hash(original));
        assertThat(anterior.isRevocado()).isTrue();
        assertThat(anterior.isReemplazado()).isTrue();
        assertThat(tabla.get(RefreshTokenService.hash(rotacion.nuevoToken())).isRevocado()).isFalse();
    }

    @Test
    @DisplayName("Un token desconocido se rechaza")
    void rotar_rechazaTokenDesconocido() {
        assertThatThrownBy(() -> service.rotar("inventado"))
                .isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("Un token vencido se rechaza")
    void rotar_rechazaTokenVencido() {
        String token = service.emitir(usuario);
        reloj.avanzar(SIETE_DIAS.plusSeconds(1));

        assertThatThrownBy(() -> service.rotar(token))
                .isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("Reutilizar un token rotado hace rato se trata como robo: se cierran todas las sesiones")
    void rotar_reutilizacionRevocaTodo() {
        String original = service.emitir(usuario);
        service.rotar(original);
        reloj.avanzar(Duration.ofMinutes(5));

        assertThatThrownBy(() -> service.rotar(original))
                .isInstanceOf(SesionInvalidaException.class);
        verify(repository).revocarTodosDelUsuario(eq(3L), any(Instant.class));
    }

    @Test
    @DisplayName("Dos pestañas renovando a la vez: se rechaza la segunda sin cerrar las demás sesiones")
    void rotar_carreraEntrePestanasNoRevocaTodo() {
        String original = service.emitir(usuario);
        service.rotar(original);
        reloj.avanzar(Duration.ofSeconds(5));

        assertThatThrownBy(() -> service.rotar(original))
                .isInstanceOf(SesionInvalidaException.class);
        verify(repository, never()).revocarTodosDelUsuario(anyLong(), any(Instant.class));
    }

    @Test
    @DisplayName("Un token cerrado con logout no puede volver a usarse")
    void revocar_invalidaElToken() {
        String token = service.emitir(usuario);

        service.revocar(token);

        assertThat(tabla.get(RefreshTokenService.hash(token)).isRevocado()).isTrue();
        assertThatThrownBy(() -> service.rotar(token))
                .isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    @DisplayName("Logout con un token desconocido no falla (idempotente)")
    void revocar_tokenDesconocidoNoFalla() {
        service.revocar("no-existe");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Un usuario desactivado no puede renovar aunque su token siga vigente")
    void rotar_rechazaUsuarioInactivo() {
        String token = service.emitir(usuario);
        usuario.setActivo(false);

        assertThatThrownBy(() -> service.rotar(token))
                .isInstanceOf(SesionInvalidaException.class);
        verify(repository).revocarTodosDelUsuario(eq(3L), any(Instant.class));
    }

    /** Reloj que la prueba puede adelantar para simular el paso del tiempo. */
    private static final class RelojAjustable extends Clock {
        private Instant ahora;

        RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
