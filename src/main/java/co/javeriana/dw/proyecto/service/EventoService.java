package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.evento.ActualizarEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.CrearEventoRequest;
import co.javeriana.dw.proyecto.dto.evento.EventoResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Evento;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EventoRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

/**
 * Eventos del diagrama: el circulo de inicio o de fin del proceso. El sistema los
 * modela, no los ejecuta.
 */
@Service
public class EventoService {

    private final EventoRepository eventoRepository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;

    public EventoService(EventoRepository eventoRepository, ProcesoRepository procesoRepository,
                         PoolRepository poolRepository, PermisoService permisoService,
                         HistorialService historialService) {
        this.eventoRepository = eventoRepository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
    }

    @Transactional
    public EventoResponse crear(CrearEventoRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.procesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);
        Pool pool = obtenerPoolDelProceso(request.poolId(), proceso);

        Evento evento = new Evento();
        evento.setNombre(request.nombre());
        evento.setTipo(request.tipo());
        evento.setPosicionX(request.posicionX());
        evento.setPosicionY(request.posicionY());
        evento.setActivo(true);
        evento.setProceso(proceso);
        evento.setPool(pool);
        evento = eventoRepository.save(evento);

        historialService.registrar("Evento", evento.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Evento creado: " + evento.getNombre());
        return EventoResponse.desde(evento);
    }

    @Transactional
    public EventoResponse actualizar(Long eventoId, ActualizarEventoRequest request, Long usuarioId) {
        Evento evento = obtenerActivo(eventoId);
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, evento.getProceso());

        evento.setNombre(request.nombre());
        evento.setTipo(request.tipo());
        evento.setPosicionX(request.posicionX());
        evento.setPosicionY(request.posicionY());
        evento = eventoRepository.save(evento);

        historialService.registrar("Evento", evento.getId(), AccionHistorial.EDICION,
                usuario, evento.getProceso(), "Evento actualizado: " + evento.getNombre());
        return EventoResponse.desde(evento);
    }

    @Transactional(readOnly = true)
    public EventoResponse obtener(Long eventoId) {
        return EventoResponse.desde(obtenerActivo(eventoId));
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return eventoRepository.findByProcesoId(procesoId).stream()
                .filter(Evento::isActivo)
                .map(EventoResponse::desde)
                .toList();
    }

    /** Eliminacion logica: el evento queda inactivo y no se pierde el historico. */
    @Transactional
    public void eliminar(Long eventoId, Long usuarioId) {
        Evento evento = obtenerActivo(eventoId);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, evento.getProceso());

        evento.setActivo(false);
        eventoRepository.save(evento);

        historialService.registrar("Evento", evento.getId(), AccionHistorial.ELIMINACION,
                usuario, evento.getProceso(), "Evento marcado como inactivo: " + evento.getNombre());
    }

    /** Un pool caja negra no tiene elementos internos modelados. */
    private Pool obtenerPoolDelProceso(Long poolId, Proceso proceso) {
        Pool pool = poolRepository.findByIdAndActivoTrue(poolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + poolId));
        if (!pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("El pool pertenece a otro proceso");
        }
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException(
                    "Un pool caja negra no puede contener elementos del diagrama");
        }
        return pool;
    }

    /** HU-03: el diagrama de un proceso solo lo modifica la empresa que lo creo. */
    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    private Evento obtenerActivo(Long id) {
        return eventoRepository.findById(id)
                .filter(Evento::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Evento no encontrado: " + id));
    }

    private Proceso obtenerProcesoActivo(Long procesoId) {
        return procesoRepository.findByIdAndEstadoNot(procesoId, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId));
    }
}
