package co.javeriana.dw.proyecto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeEnvioRequest;
import co.javeriana.dw.proyecto.dto.mensaje.CrearEventoMensajeRecepcionRequest;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.EventoMensajeRecepcion;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.TipoGateway;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.entidad.VarianteMensajeCatch;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.EventoMensajeEnvioRepository;
import co.javeriana.dw.proyecto.repository.EventoMensajeRecepcionRepository;
import co.javeriana.dw.proyecto.repository.GatewayRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

class ElementosDiagramaServiceTest {
    private final ProcesoRepository procesoRepository = mock(ProcesoRepository.class);
    private final PoolRepository poolRepository = mock(PoolRepository.class);
    private final LaneRepository laneRepository = mock(LaneRepository.class);
    private final ActividadRepository actividadRepository = mock(ActividadRepository.class);
    private final GatewayRepository gatewayRepository = mock(GatewayRepository.class);
    private final EventoMensajeEnvioRepository envioRepository = mock(EventoMensajeEnvioRepository.class);
    private final EventoMensajeRecepcionRepository recepcionRepository = mock(EventoMensajeRecepcionRepository.class);
    private final PermisoService permisoService = mock(PermisoService.class);
    private final HistorialService historialService = mock(HistorialService.class);
    private final ModelMapper modelMapper = new ModelMapper();

    private Empresa empresa;
    private Proceso proceso;
    private Usuario usuario;
    private Pool poolInterno;

    @BeforeEach
    void prepararEntidades() {
        empresa = new Empresa();
        empresa.setId(1L);
        proceso = new Proceso();
        proceso.setId(10L);
        proceso.setEmpresa(empresa);
        proceso.setEstado(EstadoProceso.BORRADOR);
        usuario = new Usuario();
        usuario.setId(100L);
        usuario.setEmpresa(empresa);
        usuario.setActivo(true);
        poolInterno = new Pool();
        poolInterno.setId(20L);
        poolInterno.setProceso(proceso);
        poolInterno.setActivo(true);
        poolInterno.setCajaNegra(false);
        when(procesoRepository.findByIdAndEstadoNot(10L, EstadoProceso.INACTIVO))
                .thenReturn(Optional.of(proceso));
        when(permisoService.validarPuedeEditar(100L)).thenReturn(usuario);
    }

    @Test
    void crearActividadDerivaElPoolDeLaLaneYRegistraHistorial() {
        Lane lane = new Lane();
        lane.setId(30L);
        lane.setPool(poolInterno);
        lane.setActivo(true);
        when(laneRepository.findByIdAndActivoTrue(30L)).thenReturn(Optional.of(lane));
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(invocacion -> {
            Actividad actividad = invocacion.getArgument(0);
            actividad.setId(40L);
            return actividad;
        });
        CrearActividadRequest request = new CrearActividadRequest();
        request.setProcesoId(10L);
        request.setLaneId(30L);
        request.setNombre("Revisar solicitud");
        request.setTipo("USUARIO");

        ActividadService service = new ActividadService(actividadRepository, procesoRepository,
                laneRepository, permisoService, historialService, modelMapper);
        service.crear(request, 100L);

        org.mockito.ArgumentCaptor<Actividad> captor = org.mockito.ArgumentCaptor.forClass(Actividad.class);
        verify(actividadRepository).save(captor.capture());
        Actividad guardada = captor.getValue();
        assertSame(poolInterno, guardada.getPool());
        assertSame(proceso, guardada.getProceso());
        verify(historialService).registrar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void gatewayRechazaPoolDeOtroProceso() {
        Proceso otroProceso = new Proceso();
        otroProceso.setId(11L);
        Pool poolAjeno = new Pool();
        poolAjeno.setId(21L);
        poolAjeno.setProceso(otroProceso);
        poolAjeno.setActivo(true);
        when(poolRepository.findByIdAndActivoTrue(21L)).thenReturn(Optional.of(poolAjeno));
        CrearGatewayRequest request = new CrearGatewayRequest();
        request.setProcesoId(10L);
        request.setPoolId(21L);
        request.setNombre("Decision");
        request.setTipo(TipoGateway.EXCLUSIVO);

        GatewayService service = new GatewayService(gatewayRepository, procesoRepository,
                poolRepository, permisoService, historialService, modelMapper);
        assertThrows(ReglaNegocioException.class, () -> service.crear(request, 100L));
    }

    @Test
    void mensajeDeEnvioRechazaOrigenYDestinoIguales() {
        when(poolRepository.findByIdAndActivoTrue(20L)).thenReturn(Optional.of(poolInterno));
        CrearEventoMensajeEnvioRequest request = new CrearEventoMensajeEnvioRequest();
        request.setProcesoId(10L);
        request.setPoolId(20L);
        request.setPoolDestinoId(20L);
        request.setNombreMensaje("Orden enviada");

        EventoMensajeEnvioService service = new EventoMensajeEnvioService(envioRepository,
                procesoRepository, poolRepository, permisoService, historialService, modelMapper);
        assertThrows(ReglaNegocioException.class, () -> service.crear(request, 100L));
    }

    @Test
    void crearRecepcionAsignaNombreYPoolObligatorios() {
        when(poolRepository.findByIdAndActivoTrue(20L)).thenReturn(Optional.of(poolInterno));
        when(recepcionRepository.save(any(EventoMensajeRecepcion.class))).thenAnswer(invocacion -> {
            EventoMensajeRecepcion evento = invocacion.getArgument(0);
            evento.setId(50L);
            return evento;
        });
        CrearEventoMensajeRecepcionRequest request = new CrearEventoMensajeRecepcionRequest();
        request.setProcesoId(10L);
        request.setPoolId(20L);
        request.setNombreMensaje("Orden recibida");
        request.setVariante(VarianteMensajeCatch.INTERMEDIO);

        EventoMensajeRecepcionService service = new EventoMensajeRecepcionService(recepcionRepository,
                procesoRepository, poolRepository, permisoService, historialService, modelMapper);
        service.crear(request, 100L);

        org.mockito.ArgumentCaptor<EventoMensajeRecepcion> captor =
                org.mockito.ArgumentCaptor.forClass(EventoMensajeRecepcion.class);
        verify(recepcionRepository).save(captor.capture());
        assertEquals("Orden recibida", captor.getValue().getNombre());
        assertSame(poolInterno, captor.getValue().getPool());
    }
}
