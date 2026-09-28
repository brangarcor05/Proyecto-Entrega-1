package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.pool.ActualizarPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.NodoProcesoRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

/**
 * Pools del diagrama (HU-21). Un pool es un participante del proceso: la empresa
 * propietaria, un cliente, un proveedor o un sistema externo. Todo elemento del
 * diagrama queda contenido en uno.
 */
@Service
public class PoolService {

    private final PoolRepository poolRepository;
    private final ProcesoRepository procesoRepository;
    private final LaneRepository laneRepository;
    private final NodoProcesoRepository nodoProcesoRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public PoolService(PoolRepository poolRepository, ProcesoRepository procesoRepository,
                       LaneRepository laneRepository, NodoProcesoRepository nodoProcesoRepository,
                       PermisoService permisoService, HistorialService historialService,
                          ModelMapper modelMapper) {
        this.poolRepository = poolRepository;
        this.procesoRepository = procesoRepository;
        this.laneRepository = laneRepository;
        this.nodoProcesoRepository = nodoProcesoRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    /**
     * Agrega un participante al diagrama. El pool de la empresa propietaria no se
     * crea por aqui: lo genera ProcesoService al crear el proceso, y es unico.
     */
    @Transactional
    public PoolResponse crear(CrearPoolRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.procesoId());
        Usuario usuario = permisoService.validarPuedeCrearPoolsYLanes(
                usuarioId, proceso.getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, proceso);

        Pool pool = new Pool();
        pool.setNombre(request.nombre());
        pool.setCajaNegra(request.cajaNegra());
        pool.setEsPropietario(false);
        pool.setActivo(true);
        pool.setProceso(proceso);
        pool = poolRepository.save(pool);

        historialService.registrar("Pool", pool.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Pool creado: " + pool.getNombre());
        return modelMapper.map(pool, PoolResponse.class);
    }

    @Transactional
    public PoolResponse actualizar(Long poolId, ActualizarPoolRequest request, Long usuarioId) {
        Pool pool = obtenerActivo(poolId);
        Usuario usuario = permisoService.validarPuedeEditarPoolsYLanes(
                usuarioId, pool.getProceso().getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, pool.getProceso());

        // HU-21: "un pool de participante externo se modela como caja negra, sin
        // elementos internos". Volver caja negra un pool que ya tiene contenido
        // dejaria el diagrama incoherente.
        boolean seVuelveCajaNegra = request.cajaNegra() && !pool.isCajaNegra();
        if (seVuelveCajaNegra && tieneContenido(poolId)) {
            throw new ReglaNegocioException(
                    "El pool tiene lanes o elementos adentro: no puede marcarse como caja negra "
                            + "hasta que se vacie");
        }

        pool.setNombre(request.nombre());
        pool.setCajaNegra(request.cajaNegra());
        pool = poolRepository.save(pool);

        historialService.registrar("Pool", pool.getId(), AccionHistorial.EDICION,
                usuario, pool.getProceso(), "Pool actualizado: " + pool.getNombre());
        return modelMapper.map(pool, PoolResponse.class);
    }

    @Transactional(readOnly = true)
    public PoolResponse obtener(Long poolId) {
        return modelMapper.map(obtenerActivo(poolId), PoolResponse.class);
    }

    @Transactional(readOnly = true)
    public List<PoolResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return poolRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(pool -> modelMapper.map(pool, PoolResponse.class))
                .toList();
    }

    /**
     * Eliminacion logica. Se bloquea en dos casos: el pool propietario dejaria al
     * proceso sin dueno, y un pool con contenido dejaria sus elementos huerfanos.
     */
    @Transactional
    public void eliminar(Long poolId, Long usuarioId) {
        Pool pool = obtenerActivo(poolId);
        Usuario usuario = permisoService.validarPuedeEliminarPoolsYLanes(
                usuarioId, pool.getProceso().getEmpresa().getId());
        validarEsDeLaEmpresa(usuario, pool.getProceso());

        if (pool.isEsPropietario()) {
            throw new ReglaNegocioException(
                    "El pool de la empresa propietaria no se puede eliminar");
        }
        if (tieneContenido(poolId)) {
            throw new ReglaNegocioException(
                    "El pool tiene lanes o elementos adentro: primero deben reasignarse o eliminarse");
        }

        pool.setActivo(false);
        poolRepository.save(pool);

        historialService.registrar("Pool", pool.getId(), AccionHistorial.ELIMINACION,
                usuario, pool.getProceso(), "Pool marcado como inactivo: " + pool.getNombre());
    }

    private boolean tieneContenido(Long poolId) {
        return laneRepository.existsByPoolIdAndActivoTrue(poolId)
                || nodoProcesoRepository.existsByPoolIdAndActivoTrue(poolId);
    }

    /** HU-03: el diagrama de un proceso solo lo modifica la empresa que lo creo. */
    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    private Pool obtenerActivo(Long id) {
        return poolRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + id));
    }

    /** Un proceso eliminado no admite cambios en su diagrama. */
    private Proceso obtenerProcesoActivo(Long procesoId) {
        return procesoRepository.findByIdAndEstadoNot(procesoId, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId));
    }
}
