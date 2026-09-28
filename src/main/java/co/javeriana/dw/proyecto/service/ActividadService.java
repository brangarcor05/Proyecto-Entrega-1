package co.javeriana.dw.proyecto.service;

import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.javeriana.dw.proyecto.dto.actividad.ActualizarActividadRequest;
import co.javeriana.dw.proyecto.dto.actividad.ActividadResponse;
import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

@Service
public class ActividadService {
    private final ActividadRepository actividadRepository;
    private final ProcesoRepository procesoRepository;
    private final LaneRepository laneRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public ActividadService(ActividadRepository actividadRepository, ProcesoRepository procesoRepository,
            LaneRepository laneRepository, PermisoService permisoService,
            HistorialService historialService, ModelMapper modelMapper) {
        this.actividadRepository = actividadRepository;
        this.procesoRepository = procesoRepository;
        this.laneRepository = laneRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ActividadResponse crear(CrearActividadRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.getProcesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);
        Lane lane = obtenerLaneDelProceso(request.getLaneId(), proceso);
        Actividad actividad = new Actividad();
        actividad.setNombre(request.getNombre());
        actividad.setTipo(request.getTipo());
        actividad.setPosicionX(request.getPosicionX());
        actividad.setPosicionY(request.getPosicionY());
        actividad.setProceso(proceso);
        actividad.setLane(lane);
        actividad.setPool(lane.getPool());
        actividad.setActivo(true);
        actividad = actividadRepository.save(actividad);
        historialService.registrar("Actividad", actividad.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Actividad creada: " + actividad.getNombre());
        return mapearAResponse(actividad);
    }

    @Transactional(readOnly = true)
    public ActividadResponse obtenerPorId(Long id) {
        return mapearAResponse(obtenerActiva(id));
    }

    @Transactional(readOnly = true)
    public List<ActividadResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return actividadRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse).toList();
    }

    @Transactional
    public ActividadResponse actualizar(Long id, ActualizarActividadRequest request, Long usuarioId) {
        Actividad actividad = obtenerActiva(id);
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, actividad.getProceso());
        Lane lane = obtenerLaneDelProceso(request.getLaneId(), actividad.getProceso());
        actividad.setNombre(request.getNombre());
        actividad.setTipo(request.getTipo());
        actividad.setLane(lane);
        actividad.setPool(lane.getPool());
        actividad.setPosicionX(request.getPosicionX());
        actividad.setPosicionY(request.getPosicionY());
        actividad = actividadRepository.save(actividad);
        historialService.registrar("Actividad", actividad.getId(), AccionHistorial.EDICION,
                usuario, actividad.getProceso(), "Actividad actualizada: " + actividad.getNombre());
        return mapearAResponse(actividad);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Actividad actividad = obtenerActiva(id);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, actividad.getProceso());
        actividad.setActivo(false);
        actividadRepository.save(actividad);
        historialService.registrar("Actividad", actividad.getId(), AccionHistorial.ELIMINACION,
                usuario, actividad.getProceso(), "Actividad marcada como inactiva: " + actividad.getNombre());
    }

    private Lane obtenerLaneDelProceso(Long laneId, Proceso proceso) {
        Lane lane = laneRepository.findByIdAndActivoTrue(laneId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + laneId));
        Pool pool = lane.getPool();
        if (!pool.isActivo() || !pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("La lane pertenece a otro proceso o su pool esta inactivo");
        }
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener actividades");
        }
        return lane;
    }

    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException("El proceso pertenece a otra empresa");
        }
    }

    private Actividad obtenerActiva(Long id) {
        return actividadRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada: " + id));
    }

    private Proceso obtenerProcesoActivo(Long id) {
        return procesoRepository.findByIdAndEstadoNot(id, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }

    private ActividadResponse mapearAResponse(Actividad actividad) {
        ActividadResponse response = modelMapper.map(actividad, ActividadResponse.class);
        response.setProcesoId(actividad.getProceso().getId());
        response.setPoolId(actividad.getPool().getId());
        response.setLaneId(actividad.getLane().getId());
        return response;
    }
}
