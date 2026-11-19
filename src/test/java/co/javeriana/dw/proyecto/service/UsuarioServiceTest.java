package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.usuario.CambiarRolRequest;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.security.RefreshTokenService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    private ModelMapper modelMapper;

    // El login (y sus pruebas) se movio a AutenticacionService / AutenticacionServiceTest.
    @Mock
    private RefreshTokenService refreshTokenService;

    // Uso el encoder real (no un mock) para poder verificar que el hash guardado
    // realmente corresponde a la contrasena en texto plano que se envio.
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder,
                          modelMapper, refreshTokenService);
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

        Empresa empresa = empresaDePrueba();  // se construye fuera del lambda

        assertThatThrownBy(() -> usuarioService.crearAdministradorInicial(
                empresa, "Juan Perez", "admin@acme.com", "claveSegura123"))
                .isInstanceOf(NombreDuplicadoException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void invitarUsuario_creaUsuarioConRolIndicado_yContrasenaSolicitada() {
        Empresa empresa = empresaDePrueba();
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Ana Ruiz");
        request.setEmail("ana@acme.com");
        request.setRolUsuario(RolUsuario.EDITOR);
        request.setPassword("claveSegura123");

        when(usuarioRepository.existsByEmail("ana@acme.com")).thenReturn(false);
        // ModelMapper esta mockeado: se simula el mapeo request -> entidad
        when(modelMapper.map(any(CrearUsuarioRequest.class), eq(Usuario.class))).thenAnswer(inv -> {
            CrearUsuarioRequest r = inv.getArgument(0);
            Usuario u = new Usuario();
            u.setNombre(r.getNombre());
            u.setEmail(r.getEmail());
            u.setRolUsuario(r.getRolUsuario());
            return u;
        });
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });

        // Configuramos el mock de modelMapper para que devuelva un UsuarioResponse
        UsuarioResponse responseSimulada = new UsuarioResponse();
        responseSimulada.setRolUsuario(RolUsuario.EDITOR);
        responseSimulada.setEmpresaId(1L);
        responseSimulada.setEmpresaNombre("Acme S.A.S.");
        when(modelMapper.map(any(Usuario.class), eq(UsuarioResponse.class))).thenReturn(responseSimulada);

        UsuarioResponse response = usuarioService.invitarUsuario(empresa, request);

        assertThat(response.getRolUsuario()).isEqualTo(RolUsuario.EDITOR);
        assertThat(response.getEmpresaId()).isEqualTo(1L);
        assertThat(response.getEmpresaNombre()).isEqualTo("Acme S.A.S.");
        verify(usuarioRepository).save(argThat(usuario ->
                passwordEncoder.matches("claveSegura123", usuario.getPasswordHash())));
    }

    @Test
    void invitarUsuario_lanzaExcepcion_cuandoCorreoYaExiste() {
        when(usuarioRepository.existsByEmail("ana@acme.com")).thenReturn(true);

        Empresa empresa = empresaDePrueba();

        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Ana Ruiz");
        request.setEmail("ana@acme.com");
        request.setRolUsuario(RolUsuario.EDITOR);

        assertThatThrownBy(() -> usuarioService.invitarUsuario(empresa, request))
                .isInstanceOf(NombreDuplicadoException.class);
    }

        @Test
    void cambiarRolAcceso_actualizaRolUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(5L);
        usuario.setRolUsuario(RolUsuario.LECTURA);
        Empresa empresa = empresaDePrueba();
        usuario.setEmpresa(empresa);
        
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        // Configuramos el mock de modelMapper
        UsuarioResponse responseSimulada = new UsuarioResponse();
        responseSimulada.setRolUsuario(RolUsuario.ADMIN);
        when(modelMapper.map(any(Usuario.class), eq(UsuarioResponse.class))).thenReturn(responseSimulada);

        UsuarioResponse response = usuarioService.cambiarRolAcceso(5L, 1L, request);

        assertThat(response.getRolUsuario()).isEqualTo(RolUsuario.ADMIN);
    }

    @Test
    void cambiarRolAcceso_lanzaExcepcion_cuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());
        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        assertThatThrownBy(() -> usuarioService.cambiarRolAcceso(99L, 1L, request))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void desactivar_marcaActivoFalse_yRevocaSusSesiones() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setActivo(true);
        usuario.setEmpresa(empresaDePrueba());
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        usuarioService.desactivar(7L, 1L);

        assertThat(usuario.isActivo()).isFalse();
        verify(usuarioRepository).save(usuario);
        // Sin esto, el usuario desactivado podria seguir renovando su sesion 7 dias.
        verify(refreshTokenService).revocarTodos(7L);
    }

    @Test
    void listarPorEmpresa_devuelveListaDeResponses() {
        Usuario u1 = new Usuario();
        u1.setId(1L);
        u1.setEmpresa(empresaDePrueba());
        when(usuarioRepository.findByEmpresaId(1L)).thenReturn(List.of(u1));

        // Configuramos el mock de modelMapper
        UsuarioResponse responseSimulada = new UsuarioResponse();
        responseSimulada.setId(1L);
        when(modelMapper.map(any(Usuario.class), eq(UsuarioResponse.class))).thenReturn(responseSimulada);

        List<UsuarioResponse> resultado = usuarioService.listarPorEmpresa(1L);

        assertThat(resultado).hasSize(1);
    }
}
