package co.javeriana.dw.proyecto.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

/**
 * Aislamiento por empresa para todo lo que cuelga de un proceso (pools, lanes, nodos,
 * arcos, mensajes...). La empresa siempre llega del access token, nunca del cliente.
 *
 * - Lectura: la empresa propietaria o una empresa a la que se le compartio (HU-23).
 *   Un proceso ajeno no compartido se reporta como inexistente (404) para no revelar
 *   que existe.
 * - Escritura: solo la empresa propietaria; el acceso compartido es de solo lectura.
 */
@Service
public class AccesoProcesoService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;

    public AccesoProcesoService(ProcesoRepository procesoRepository, PoolRepository poolRepository) {
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
    }

    /** Proceso activo (no eliminado logicamente). */
    @Transactional(readOnly = true)
    public Proceso obtenerProcesoActivo(Long procesoId) {
        return procesoRepository.findByIdAndEstadoNot(procesoId, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId));
    }

    /** Devuelve el proceso si la empresa del token puede consultarlo; si no, 404. */
    @Transactional(readOnly = true)
    public Proceso validarLectura(Long procesoId, Long empresaId) {
        Proceso proceso = obtenerProcesoActivo(procesoId);
        if (!puedeConsultar(proceso, empresaId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId);
        }
        return proceso;
    }

    @Transactional(readOnly = true)
    public boolean puedeConsultar(Proceso proceso, Long empresaId) {
        if (empresaId == null) {
            return false;
        }
        return proceso.getEmpresa().getId().equals(empresaId)
                || (proceso.isCompartido()
                        && procesoRepository.existsByIdAndEmpresasCompartidasId(proceso.getId(), empresaId));
    }

    /** HU-03 y HU-23: el diagrama solo lo modifica la empresa que creo el proceso. */
    public void validarEscritura(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException(
                    "El proceso pertenece a otra empresa: el acceso compartido es de solo lectura");
        }
    }

    /**
     * Pool activo que pertenece al proceso indicado. Evita que un nodo quede en un
     * pool de otro proceso (o de otra empresa) con solo cambiar el id en la peticion.
     */
    @Transactional(readOnly = true)
    public Pool obtenerPoolDelProceso(Long poolId, Proceso proceso) {
        if (poolId == null) {
            throw new ReglaNegocioException("El pool es obligatorio");
        }
        Pool pool = poolRepository.findByIdAndActivoTrue(poolId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + poolId));
        if (!pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("El pool pertenece a otro proceso");
        }
        return pool;
    }
}
