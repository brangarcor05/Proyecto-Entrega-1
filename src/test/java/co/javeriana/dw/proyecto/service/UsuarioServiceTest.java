package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.config.ModelMapperConfig;
import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.dto.usuario.CambiarRolRequest;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la capa de servicio de Usuario: registro, invitacion, cambio de rol,
 * desactivacion y autenticacion (HU-02, HU-03). No tocan base de datos: UsuarioRepository
 * esta mockeado, asi que corren en milisegundos y no dependen de Supabase ni de H2.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioService usuarioService;

    // Uso el encoder real (no un mock) para poder verificar que el hash guardado
    // realmente corresponde a la contrasena en texto plano que se envio.
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder,
                new ModelMapperConfig().modelMapper());
    }

    private Empresa empresaDePrueba() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Acme S.A.S.");
        return empresa;
    }

    @Test
    void crearAdministradorInicial_creaConRolAdmin_cuandoCorreoNoExiste() {
        Empresa empresa = empresaDePrueba();
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        Usuario resultado = usuarioService.crearAdministradorInicial(
                empresa, "Juan Perez", "admin@acme.com", "claveSegura123");

        assertThat(resultado.getRolUsuario()).isEqualTo(RolUsuario.ADMIN);
        assertThat(resultado.isActivo()).isTrue();
        assertThat(passwordEncoder.matches("claveSegura123", resultado.getPasswordHash())).isTrue();
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @Test
    void crearAdministradorInicial_lanzaExcepcion_cuandoCorreoYaExiste() {
        when(usuarioRepository.existsByEmail("admin@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.crearAdministradorInicial(
                empresaDePrueba(), "Juan Perez", "admin@acme.com", "claveSegura123"))
                .isInstanceOf(NombreDuplicadoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void invitarUsuario_creaUsuarioConRolIndicado_yContrasenaAleatoria() {
        Empresa empresa = empresaDePrueba();
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Ana Ruiz");
        request.setEmail("ana@acme.com");
        request.setRolUsuario(RolUsuario.EDITOR);

        when(usuarioRepository.existsByEmail("ana@acme.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });

        UsuarioResponse response = usuarioService.invitarUsuario(empresa, request);

        assertThat(response.getRolUsuario()).isEqualTo(RolUsuario.EDITOR);
        assertThat(response.getEmpresaId()).isEqualTo(1L);
        assertThat(response.getEmpresaNombre()).isEqualTo("Acme S.A.S.");
    }

    @Test
    void invitarUsuario_lanzaExcepcion_cuandoCorreoYaExiste() {
        when(usuarioRepository.existsByEmail("ana@acme.com")).thenReturn(true);
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Ana Ruiz");
        request.setEmail("ana@acme.com");
        request.setRolUsuario(RolUsuario.EDITOR);

        assertThatThrownBy(() -> usuarioService.invitarUsuario(empresaDePrueba(), request))
                .isInstanceOf(NombreDuplicadoException.class);
    }

    @Test
    void cambiarRolAcceso_actualizaRolUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setRolUsuario(RolUsuario.LECTURA);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        UsuarioResponse response = usuarioService.cambiarRolAcceso(5L, request);

        assertThat(response.getRolUsuario()).isEqualTo(RolUsuario.ADMIN);
    }

    @Test
    void cambiarRolAcceso_lanzaExcepcion_cuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        assertThatThrownBy(() -> usuarioService.cambiarRolAcceso(99L, request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivar_marcaActivoFalse() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setActivo(true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        usuarioService.desactivar(7L);

        assertThat(usuario.isActivo()).isFalse();
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void autenticar_devuelveSesion_cuandoCredencialesValidas() {
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setEmail("juan@acme.com");
        usuario.setPasswordHash(passwordEncoder.encode("claveCorrecta"));
        usuario.setActivo(true);
        usuario.setEmpresa(empresaDePrueba());
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.of(usuario));

        LoginRequest login = new LoginRequest();
        login.setEmail("juan@acme.com");
        login.setPassword("claveCorrecta");

        SesionResponse sesion = usuarioService.autenticar(login);

        assertThat(sesion.getUsuarioId()).isEqualTo(3L);
        assertThat(sesion.getEmpresaId()).isEqualTo(1L);
    }

    @Test
    void autenticar_lanzaCredencialesInvalidas_cuandoCorreoNoExiste() {
        when(usuarioRepository.findByEmail("nadie@acme.com")).thenReturn(Optional.empty());
        LoginRequest login = new LoginRequest();
        login.setEmail("nadie@acme.com");
        login.setPassword("algo");

        assertThatThrownBy(() -> usuarioService.autenticar(login))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void autenticar_lanzaCredencialesInvalidas_cuandoPasswordIncorrecta() {
        Usuario usuario = new Usuario();
        usuario.setEmail("juan@acme.com");
        usuario.setPasswordHash(passwordEncoder.encode("claveCorrecta"));
        usuario.setActivo(true);
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.of(usuario));

        LoginRequest login = new LoginRequest();
        login.setEmail("juan@acme.com");
        login.setPassword("claveIncorrecta");

        assertThatThrownBy(() -> usuarioService.autenticar(login))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void autenticar_lanzaCredencialesInvalidas_cuandoUsuarioInactivo() {
        // HU-02/HU-03: un usuario desactivado no puede iniciar sesion aunque la
        // contrasena sea correcta.
        Usuario usuario = new Usuario();
        usuario.setEmail("juan@acme.com");
        usuario.setPasswordHash(passwordEncoder.encode("claveCorrecta"));
        usuario.setActivo(false);
        when(usuarioRepository.findByEmail("juan@acme.com")).thenReturn(Optional.of(usuario));

        LoginRequest login = new LoginRequest();
        login.setEmail("juan@acme.com");
        login.setPassword("claveCorrecta");

        assertThatThrownBy(() -> usuarioService.autenticar(login))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void listarPorEmpresa_devuelveListaDeResponses() {
        Usuario u1 = new Usuario();
        u1.setId(1L);
        u1.setEmpresa(empresaDePrueba());
        when(usuarioRepository.findByEmpresaId(1L)).thenReturn(List.of(u1));

        List<UsuarioResponse> resultado = usuarioService.listarPorEmpresa(1L);

        assertThat(resultado).hasSize(1);
    }
}