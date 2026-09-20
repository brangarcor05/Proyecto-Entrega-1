package co.javeriana.dw.proyecto.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.lane.ActualizarLaneRequest;
import co.javeriana.dw.proyecto.dto.lane.CrearLaneRequest;
import co.javeriana.dw.proyecto.dto.lane.LaneResponse;
import co.javeriana.dw.proyecto.dto.lane.ReordenarLanesRequest;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.RolProceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import co.javeriana.dw.proyecto.repository.RolProcesoRepository;

/**
 * Lanes del diagrama (HU-22): la banda dentro de un pool asociada a un rol de
 * proceso. No lleva nombre propio, su etiqueta es la del rol.
 *
 * A diferencia de RolProceso, HU-24 sí cubre lanes explícitamente ("qué roles de
 * acceso pueden crear, editar o eliminar pools y lanes"), así que acá se usan los
 * métodos *PoolsYLanes de PermisoService, igual que PoolService.
 */
@Service
public class LaneService {

    private final LaneRepository laneRepository;
    private final PoolRepository poolRepository;
    private final RolProcesoRepository rolProcesoRepository;
    private final ProcesoRepository procesoRepository;
    private final ActividadRepository actividadRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;

    public LaneService(LaneRepository laneRepository, PoolRepository poolRepository,
                       RolProcesoRepository rolProcesoRepository, ProcesoRepository procesoRepository,
                       ActividadRepository actividadRepository, PermisoService permisoService,
                       HistorialService historialService) {
        this.laneRepository = laneRepository;
        this.poolRepository = poolRepository;
        this.rolProcesoRepository = rolProcesoRepository;
        this.procesoRepository = procesoRepository;
        this.actividadRepository = actividadRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
    }

    @Transactional
    public LaneResponse crear(CrearLaneRequest request, Long usuarioId) {
        Pool pool = obtenerPoolActivo(request.poolId());
        Proceso proceso = pool.getProceso();
        Usuario usuario = permisoService.validarPuedeCrearPoolsYLanes(usuarioId, proceso.getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, proceso);

        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede tener lanes");
        }

        RolProceso rol = obtenerRolActivo(request.rolProcesoId(), proceso.getEmpresa().getId());

        if (laneRepository.existsByPoolIdAndRolProcesoId(pool.getId(), rol.getId())) {
            throw new ReglaNegocioException(
                    "Ya existe una lane con ese rol en este pool");
        }

        int siguienteOrden = laneRepository.findByPoolIdAndActivoTrueOrderByOrden(pool.getId()).size();

        Lane lane = new Lane();
        lane.setPool(pool);
        lane.setRolProceso(rol);
        lane.setOrden(siguienteOrden);
        lane.setActivo(true);
        lane = laneRepository.save(lane);

        historialService.registrar("Lane", lane.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Lane creada para el rol: " + rol.getNombre());
        return LaneResponse.desde(lane);
    }

    /** HU-22: "renombrar" una lane es reasignarle otro rol, porque no tiene nombre propio. */
    @Transactional
    public LaneResponse actualizar(Long laneId, ActualizarLaneRequest request, Long usuarioId) {
        Lane lane = obtenerActiva(laneId);
        Pool pool = lane.getPool();
        Proceso proceso = pool.getProceso();
        Usuario usuario = permisoService.validarPuedeEditarPoolsYLanes(usuarioId, proceso.getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, proceso);

        RolProceso nuevoRol = obtenerRolActivo(request.rolProcesoId(), proceso.getEmpresa().getId());

        boolean cambiaDeRol = !nuevoRol.getId().equals(lane.getRolProceso().getId());
        if (cambiaDeRol && laneRepository.existsByPoolIdAndRolProcesoId(pool.getId(), nuevoRol.getId())) {
            throw new ReglaNegocioException("Ya existe una lane con ese rol en este pool");
        }

        lane.setRolProceso(nuevoRol);
        lane = laneRepository.save(lane);

        historialService.registrar("Lane", lane.getId(), AccionHistorial.EDICION,
                usuario, proceso, "Lane reasignada al rol: " + nuevoRol.getNombre());
        return LaneResponse.desde(lane);
    }

    /**
     * HU-22: reordenar es una operación de conjunto sobre todas las lanes activas
     * del pool, no sobre una lane individual.
     */
    @Transactional
    public List<LaneResponse> reordenar(Long poolId, ReordenarLanesRequest request, Long usuarioId) {
        Pool pool = obtenerPoolActivo(poolId);
        Proceso proceso = pool.getProceso();
        Usuario usuario = permisoService.validarPuedeEditarPoolsYLanes(usuarioId, proceso.getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, proceso);

        List<Lane> lanesActuales = laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId);
        List<Long> idsRecibidos = request.laneIdsEnOrden();
        List<Long> idsActuales = lanesActuales.stream().map(Lane::getId).toList();

        if (idsRecibidos.size() != idsActuales.size() || !idsRecibidos.containsAll(idsActuales)) {
            throw new ReglaNegocioException(
                    "La lista debe incluir exactamente las lanes activas del pool, sin repetir ni omitir ninguna");
        }

        List<Lane> actualizadas = new ArrayList<>();
        for (int posicion = 0; posicion < idsRecibidos.size(); posicion++) {
            Long laneId = idsRecibidos.get(posicion);
            Lane lane = lanesActuales.stream()
                    .filter(l -> l.getId().equals(laneId))
                    .findFirst()
                    .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + laneId));
            lane.setOrden(posicion);
            actualizadas.add(laneRepository.save(lane));
        }

        historialService.registrar("Lane", pool.getId(), AccionHistorial.EDICION,
                usuario, proceso, "Lanes reordenadas en el pool: " + pool.getNombre());
        return actualizadas.stream().map(LaneResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public List<LaneResponse> listarPorPool(Long poolId) {
        obtenerPoolActivo(poolId);
        return laneRepository.findByPoolIdAndActivoTrueOrderByOrden(poolId).stream()
                .map(LaneResponse::desde)
                .toList();
    }

    /**
     * HU-22: "no se puede eliminar una lane que contenga actividades: primero deben
     * reasignarse a otra lane."
     */
    @Transactional
    public void eliminar(Long laneId, Long usuarioId) {
        Lane lane = obtenerActiva(laneId);
        Proceso proceso = lane.getPool().getProceso();
        Usuario usuario = permisoService.validarPuedeEliminarPoolsYLanes(usuarioId, proceso.getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, proceso);

        if (actividadRepository.existsByLaneIdAndActivoTrue(laneId)) {
            throw new ReglaNegocioException(
                    "La lane tiene actividades asignadas: reasígnelas a otra lane antes de eliminarla");
        }

        lane.setActivo(false);
        laneRepository.save(lane);

        historialService.registrar("Lane", lane.getId(), AccionHistorial.ELIMINACION,
                usuario, proceso, "Lane eliminada (rol " + lane.getRolProceso().getNombre() + ")");
    }

    private RolProceso obtenerRolActivo(Long rolId, Long empresaId) {
        RolProceso rol = rolProcesoRepository.findByIdAndActivoTrue(rolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol de proceso no encontrado: " + rolId));
        // HU-22: "cada lane se asocia a un rol de proceso de la empresa" - no de otra.
        if (!rol.getEmpresa().getId().equals(empresaId)) {
            throw new ReglaNegocioException("El rol de proceso pertenece a otra empresa");
        }
        return rol;
    }

    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    private Pool obtenerPoolActivo(Long poolId) {
        Pool pool = poolRepository.findByIdAndActivoTrue(poolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + poolId));
        obtenerProcesoActivo(pool.getProceso().getId());
        return pool;
    }

    private Lane obtenerActiva(Long id) {
        return laneRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + id));
    }

    private Proceso obtenerProcesoActivo(Long procesoId) {
        return procesoRepository.findByIdAndEstadoNot(procesoId, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId));
    }
}
