package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.actividad.ActualizarActividadRequest;
import co.javeriana.dw.proyecto.dto.actividad.ActividadResponse;
import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Actividades del diagrama (HU-08 a HU-10).
 *
 * - Crear y editar: administrador o editor. Eliminar: solo administrador.
 * - Solo la empresa propietaria del proceso modifica su diagrama; las invitadas (HU-23)
 *   solo consultan.
 * - La actividad pertenece a una lane del pool indicado, y el pool al proceso (HU-08 y
 *   HU-22: las actividades se asignan a funciones, no a personas).
 * - El nombre es unico dentro del proceso (HU-08).
 * - Cada cambio queda en el historial del proceso.
 */
@Service
public class ActividadService {

    private static final String ENTIDAD = "Actividad";

    private final ActividadRepository actividadRepository;
    private final LaneRepository laneRepository;
    private final PermisoService permisoService;
    private final AccesoProcesoService accesoProcesoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public ActividadService(ActividadRepository actividadRepository,
                            LaneRepository laneRepository,
                            PermisoService permisoService,
                            AccesoProcesoService accesoProcesoService,
                            HistorialService historialService,
                            ModelMapper modelMapper) {
        this.actividadRepository = actividadRepository;
        this.laneRepository = laneRepository;
        this.permisoService = permisoService;
        this.accesoProcesoService = accesoProcesoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ActividadResponse crear(CrearActividadRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        Proceso proceso = accesoProcesoService.obtenerProcesoActivo(request.getProcesoId());
        accesoProcesoService.validarEscritura(usuario, proceso);

        Pool pool = obtenerPoolConElementos(request.getPoolId(), proceso);
        Lane lane = obtenerLaneDelPool(request.getLaneId(), pool);

        String nombre = request.getNombre().trim();
        if (actividadRepository.existsByProcesoIdAndNombreIgnoreCaseAndActivoTrue(proceso.getId(), nombre)) {
            throw new NombreDuplicadoException("Ya existe una actividad llamada '" + nombre + "' en este proceso");
        }

        Actividad actividad = modelMapper.map(request, Actividad.class);
        actividad.setId(null);
        actividad.setNombre(nombre);
        actividad.setProceso(proceso);
        actividad.setPool(pool);
        actividad.setLane(lane);
        actividad.setActivo(true);
        actividad.setCanvasId(request.getCanvasId());

        if (actividad.getPosicionX() == null) actividad.setPosicionX(0.0);
        if (actividad.getPosicionY() == null) actividad.setPosicionY(0.0);

        actividad = actividadRepository.save(actividad);

        historialService.registrar(ENTIDAD, actividad.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Actividad creada: " + actividad.getNombre()
                        + " (lane " + lane.getRolProceso().getNombre() + ")");
        return mapearAResponse(actividad);
    }

    @Transactional(readOnly = true)
    public ActividadResponse obtenerPorId(Long id, Long empresaId) {
        Actividad actividad = obtenerActiva(id);
        accesoProcesoService.validarLectura(actividad.getProceso().getId(), empresaId);
        return mapearAResponse(actividad);
    }

    @Transactional(readOnly = true)
    public List<ActividadResponse> listarPorProceso(Long procesoId, Long empresaId) {
        accesoProcesoService.validarLectura(procesoId, empresaId);
        return actividadRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public ActividadResponse actualizar(Long id, ActualizarActividadRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        Actividad actividad = obtenerActiva(id);
        Proceso proceso = actividad.getProceso();
        accesoProcesoService.validarEscritura(usuario, proceso);

        Pool pool = request.getPoolId() != null
                ? obtenerPoolConElementos(request.getPoolId(), proceso)
                : actividad.getPool();
        Lane lane = obtenerLaneDelPool(request.getLaneId(), pool);

        String nombre = request.getNombre().trim();
        if (actividadRepository.existsByProcesoIdAndNombreIgnoreCaseAndActivoTrueAndIdNot(
                proceso.getId(), nombre, actividad.getId())) {
            throw new NombreDuplicadoException("Ya existe una actividad llamada '" + nombre + "' en este proceso");
        }

        actividad.setPool(pool);
        actividad.setLane(lane);
        actividad.setNombre(nombre);
        actividad.setTipo(request.getTipo());

        if (request.getPosicionX() != null) actividad.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null) actividad.setPosicionY(request.getPosicionY());
        if (request.getCanvasId() != null) actividad.setCanvasId(request.getCanvasId());

        actividad = actividadRepository.save(actividad);

        historialService.registrar(ENTIDAD, actividad.getId(), AccionHistorial.EDICION,
                usuario, proceso, "Actividad actualizada: " + actividad.getNombre());
        return mapearAResponse(actividad);
    }

    /** HU-10: eliminacion logica, solo el administrador. */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        Actividad actividad = obtenerActiva(id);
        accesoProcesoService.validarEscritura(usuario, actividad.getProceso());

        actividad.setActivo(false);
        actividadRepository.save(actividad);

        historialService.registrar(ENTIDAD, actividad.getId(), AccionHistorial.ELIMINACION,
                usuario, actividad.getProceso(), "Actividad marcada como inactiva: " + actividad.getNombre());
    }

    private Actividad obtenerActiva(Long id) {
        return actividadRepository.findById(id)
                .filter(Actividad::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada: " + id));
    }

    /** Pool del mismo proceso que no sea caja negra (un participante externo no tiene actividades). */
    private Pool obtenerPoolConElementos(Long poolId, Proceso proceso) {
        Pool pool = accesoProcesoService.obtenerPoolDelProceso(poolId, proceso);
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener actividades");
        }
        return pool;
    }

    /** HU-22: la lane debe existir, estar activa y pertenecer al pool de la actividad. */
    private Lane obtenerLaneDelPool(Long laneId, Pool pool) {
        if (laneId == null) {
            throw new ReglaNegocioException("La actividad debe pertenecer a una lane");
        }
        Lane lane = laneRepository.findByIdAndActivoTrue(laneId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + laneId));
        if (!lane.getPool().getId().equals(pool.getId())) {
            throw new ReglaNegocioException("La lane no pertenece al pool indicado");
        }
        return lane;
    }

    private ActividadResponse mapearAResponse(Actividad actividad) {
        ActividadResponse response = modelMapper.map(actividad, ActividadResponse.class);
        if (actividad.getProceso() != null) response.setProcesoId(actividad.getProceso().getId());
        if (actividad.getPool() != null)    response.setPoolId(actividad.getPool().getId());
        if (actividad.getLane() != null)    response.setLaneId(actividad.getLane().getId());
        if (actividad.getCanvasId() != null) response.setCanvasId(actividad.getCanvasId());
        return response;
    }
}
