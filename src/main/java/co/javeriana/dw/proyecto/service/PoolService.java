package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.pool.ActualizarPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.CrearPoolRequest;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.NodoProcesoRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;

/**
 * Pools del diagrama (HU-21). Un pool es un participante del proceso: la empresa
 * propietaria, un cliente, un proveedor o un sistema externo. Todo elemento del
 * diagrama queda contenido en uno.
 */
@Service
public class PoolService {

    private final PoolRepository poolRepository;
    private final ProcesoRepository procesoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LaneRepository laneRepository;
    private final NodoProcesoRepository nodoProcesoRepository;
    private final HistorialService historialService;

    public PoolService(PoolRepository poolRepository, ProcesoRepository procesoRepository,
                       UsuarioRepository usuarioRepository, LaneRepository laneRepository,
                       NodoProcesoRepository nodoProcesoRepository, HistorialService historialService) {
        this.poolRepository = poolRepository;
        this.procesoRepository = procesoRepository;
        this.usuarioRepository = usuarioRepository;
        this.laneRepository = laneRepository;
        this.nodoProcesoRepository = nodoProcesoRepository;
        this.historialService = historialService;
    }

    /**
     * Agrega un participante al diagrama. El pool de la empresa propietaria no se
     * crea por aqui: lo genera ProcesoService al crear el proceso, y es unico.
     */
    @Transactional
    public PoolResponse crear(CrearPoolRequest request, Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        validarPuedeEditar(usuario);

        Proceso proceso = procesoRepository.findById(request.procesoId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe el proceso " + request.procesoId()));

        Pool pool = new Pool();
        pool.setNombre(request.nombre());
        pool.setCajaNegra(request.cajaNegra());
        pool.setEsPropietario(false);
        pool.setActivo(true);
        pool.setProceso(proceso);
        pool = poolRepository.save(pool);

        historialService.registrar("Pool", pool.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Pool creado: " + pool.getNombre());
        return PoolResponse.desde(pool);
    }

    @Transactional
    public PoolResponse actualizar(Long poolId, ActualizarPoolRequest request, Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        validarPuedeEditar(usuario);
        Pool pool = obtenerActivo(poolId);

        // HU-21: "un pool de participante externo se modela como caja negra, sin
        // elementos internos". Volver caja negra un pool que ya tiene contenido
        // dejaria el diagrama incoherente.
        boolean seVuelveCajaNegra = request.cajaNegra() && !pool.isCajaNegra();
        if (seVuelveCajaNegra && tieneContenido(poolId)) {
            throw new PermisoDenegadoException(
                    "El pool tiene lanes o elementos adentro: no puede marcarse como caja negra "
                            + "hasta que se vacie");
        }

        pool.setNombre(request.nombre());
        pool.setCajaNegra(request.cajaNegra());
        pool = poolRepository.save(pool);

        historialService.registrar("Pool", pool.getId(), AccionHistorial.EDICION,
                usuario, pool.getProceso(), "Pool actualizado: " + pool.getNombre());
        return PoolResponse.desde(pool);
    }

    @Transactional(readOnly = true)
    public PoolResponse obtener(Long poolId) {
        return PoolResponse.desde(obtenerActivo(poolId));
    }

    @Transactional(readOnly = true)
    public List<PoolResponse> listarPorProceso(Long procesoId) {
        if (!procesoRepository.existsById(procesoId)) {
            throw new RecursoNoEncontradoException("No existe el proceso " + procesoId);
        }
        return poolRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(PoolResponse::desde)
                .toList();
    }

    /**
     * Eliminacion logica. Se bloquea en dos casos: el pool propietario dejaria al
     * proceso sin dueno, y un pool con contenido dejaria sus elementos huerfanos.
     */
    @Transactional
    public void eliminar(Long poolId, Long usuarioId) {
        Usuario usuario = obtenerUsuario(usuarioId);
        if (usuario.getRolUsuario() != RolUsuario.ADMIN) {
            throw new PermisoDenegadoException("Solo un administrador puede eliminar pools");
        }
        Pool pool = obtenerActivo(poolId);

        if (pool.isEsPropietario()) {
            throw new PermisoDenegadoException(
                    "El pool de la empresa propietaria no se puede eliminar");
        }
        if (tieneContenido(poolId)) {
            throw new PermisoDenegadoException(
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

    /** HU-21: "solo usuarios con permisos de edicion pueden crear o modificar pools". */
    private void validarPuedeEditar(Usuario usuario) {
        boolean puedeEditar = usuario.getRolUsuario() == RolUsuario.ADMIN
                || usuario.getRolUsuario() == RolUsuario.EDITOR;
        if (!puedeEditar) {
            throw new PermisoDenegadoException("Los usuarios de solo lectura no pueden modificar pools");
        }
    }

    private Pool obtenerActivo(Long id) {
        return poolRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + id));
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}
