package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.SesionInvalidaException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.security.JwtService;
import co.javeriana.dw.proyecto.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** HU-03: login con access + refresh token, renovación y logout. */
@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ModelMapper modelMapper;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AutenticacionService service;

    @BeforeEach
    void setUp() {
        service = new AutenticacionService(usuarioRepository, passwordEncoder, modelMapper,
                jwtService, refreshTokenService);
    }

    private Usuario usuario(boolean activo) {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Acme S.A.S.");
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setEmail("juan@acme.com");
        usuario.setPasswordHash(passwordEncoder.encode("claveCorrecta"));
        usuario.setRolUsuario(RolUsuario.EDITOR);
        usuario.setActivo(activo);
        usuario.setEmpresa(empresa);
        return usuario;
    }

    private LoginRequest login(String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail("juan@acme.com");
        request.setPassword(password);
        return request;
    }

    private void simularMapeo() {
        SesionResponse base = new SesionResponse();
        base.setUsuarioId(3L);
        base.setEmpresaId(1L);
        when(modelMapper.map(any(Usuario.class), eq(SesionResponse.class))).thenReturn(base);
    }

    @Test
    void iniciarSesion_entregaAccessYRefreshToken() {
        Usuario usuario = usuario(true);
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.of(usuario));
        simularMapeo();
        when(jwtService.generarAccessToken(usuario)).thenReturn("access-jwt");
        when(jwtService.getExpiracionSegundos()).thenReturn(900L);
        when(refreshTokenService.emitir(usuario)).thenReturn("refresh-opaco");
        when(refreshTokenService.getDuracionSegundos()).thenReturn(604800L);

        SesionResponse sesion = service.iniciarSesion(login("claveCorrecta"));

        assertThat(sesion.getUsuarioId()).isEqualTo(3L);
        assertThat(sesion.getTokenType()).isEqualTo("Bearer");
        assertThat(sesion.getAccessToken()).isEqualTo("access-jwt");
        assertThat(sesion.getExpiresIn()).isEqualTo(900L);
        assertThat(sesion.getRefreshToken()).isEqualTo("refresh-opaco");
        assertThat(sesion.getRefreshExpiresIn()).isEqualTo(604800L);
    }

    @Test
    void iniciarSesion_rechazaCorreoInexistente() {
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.iniciarSesion(login("algo")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(refreshTokenService, never()).emitir(any());
    }

    @Test
    void iniciarSesion_rechazaPasswordIncorrecta() {
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.of(usuario(true)));

        assertThatThrownBy(() -> service.iniciarSesion(login("claveIncorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class);
        verify(refreshTokenService, never()).emitir(any());
    }

    @Test
    void iniciarSesion_rechazaUsuarioInactivo() {
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.of(usuario(false)));

        assertThatThrownBy(() -> service.iniciarSesion(login("claveCorrecta")))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void renovar_entregaUnParNuevoConLosDatosActualesDelUsuario() {
        Usuario usuario = usuario(true);
        when(refreshTokenService.rotar("refresh-viejo"))
                .thenReturn(new RefreshTokenService.Rotacion(usuario, "refresh-nuevo"));
        simularMapeo();
        when(jwtService.generarAccessToken(usuario)).thenReturn("access-nuevo");

        SesionResponse sesion = service.renovar("refresh-viejo");

        assertThat(sesion.getAccessToken()).isEqualTo("access-nuevo");
        assertThat(sesion.getRefreshToken()).isEqualTo("refresh-nuevo");
    }

    @Test
    void renovar_propagaSesionInvalida() {
        when(refreshTokenService.rotar("vencido")).thenThrow(new SesionInvalidaException());

        assertThatThrownBy(() -> service.renovar("vencido"))
                .isInstanceOf(SesionInvalidaException.class);
    }

    @Test
    void cerrarSesion_revocaElRefreshToken() {
        service.cerrarSesion("refresh-1");
        verify(refreshTokenService).revocar("refresh-1");
    }
}
