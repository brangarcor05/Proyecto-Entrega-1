package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.config.ModelMapperConfig;
import co.javeriana.dw.proyecto.dto.empresa.CrearEmpresaRequest;
import co.javeriana.dw.proyecto.dto.empresa.EmpresaResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas de la capa de servicio de Empresa: registro (HU-01), que incluye la
 * creacion del administrador inicial en la misma operacion. UsuarioService esta
 * mockeado a proposito: aqui solo interesa el comportamiento de EmpresaService,
 * no repetir las pruebas de UsuarioService.
 */
@ExtendWith(MockitoExtension.class)
class EmpresaServiceTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private UsuarioService usuarioService;

    private EmpresaService empresaService;

    @BeforeEach
    void setUp() {
        empresaService = new EmpresaService(empresaRepository, usuarioService,
                new ModelMapperConfig().modelMapper());
    }

    private CrearEmpresaRequest requestDePrueba() {
        CrearEmpresaRequest request = new CrearEmpresaRequest();
        request.setNombre("Acme S.A.S.");
        request.setRuc("900123456-7");
        request.setRazonSocial("Acme Sociedad Anonima");
        request.setEmail("contacto@acme.com");
        request.setTelefono("3001234567");
        request.setDireccion("Calle 1 #2-3");
        request.setSector("Tecnologia");
        request.setAdminNombre("Juan Perez");
        request.setAdminPassword("claveSegura123");
        return request;
    }

    @Test
    void crear_creaEmpresaYAdministrador_enUnaSolaLlamada() {
        CrearEmpresaRequest request = requestDePrueba();
        when(empresaRepository.existsByRuc(request.getRuc())).thenReturn(false);
        when(empresaRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(empresaRepository.save(any(Empresa.class))).thenAnswer(inv -> {
            Empresa e = inv.getArgument(0);
            if (e.getId() == null) e.setId(1L);
            return e;
        });
        Usuario adminCreado = new Usuario();
        adminCreado.setId(99L);
        when(usuarioService.crearAdministradorInicial(any(), eq("Juan Perez"), eq("contacto@acme.com"),
                eq("claveSegura123"))).thenReturn(adminCreado);

        EmpresaResponse response = empresaService.crear(request);

        assertThat(response.getAdminUsuarioId()).isEqualTo(99L);
        assertThat(response.getNombre()).isEqualTo("Acme S.A.S.");
        // se guarda dos veces: una para obtener el id, otra despues de setAdminUsuarioId
        verify(empresaRepository, times(2)).save(any(Empresa.class));
    }

    @Test
    void crear_lanzaExcepcion_cuandoRucYaExiste() {
        CrearEmpresaRequest request = requestDePrueba();
        when(empresaRepository.existsByRuc(request.getRuc())).thenReturn(true);

        assertThatThrownBy(() -> empresaService.crear(request))
                .isInstanceOf(NombreDuplicadoException.class);

        verifyNoInteractions(usuarioService);
    }

    @Test
    void crear_lanzaExcepcion_cuandoEmailYaExiste() {
        CrearEmpresaRequest request = requestDePrueba();
        when(empresaRepository.existsByRuc(request.getRuc())).thenReturn(false);
        when(empresaRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> empresaService.crear(request))
                .isInstanceOf(NombreDuplicadoException.class);

        verify(empresaRepository, never()).save(any());
    }

    @Test
    void obtenerPorId_devuelveEmpresa_cuandoExiste() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        empresa.setNombre("Acme S.A.S.");
        when(empresaRepository.findById(1L)).thenReturn(Optional.of(empresa));

        EmpresaResponse response = empresaService.obtenerPorId(1L);

        assertThat(response.getNombre()).isEqualTo("Acme S.A.S.");
    }

    @Test
    void obtenerPorId_lanzaExcepcion_cuandoNoExiste() {
        when(empresaRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> empresaService.obtenerPorId(404L))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}