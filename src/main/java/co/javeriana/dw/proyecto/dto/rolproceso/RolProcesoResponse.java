package co.javeriana.dw.proyecto.dto.rolproceso;

import java.util.List;

import co.javeriana.dw.proyecto.entidad.RolProceso;

/**
 * Un rol de proceso. Aplana la empresa a su id, igual que PoolResponse aplana el
 * proceso: cargar la entidad completa arrastraría la lista de roles de la empresa.
 *
 * enUso y procesos responden directamente a HU-20 ("el listado indica si el rol
 * puede eliminarse o si está en uso" y "para cada rol se puede ver en qué procesos
 * está siendo usado"), por eso no salen de RolProceso.desde(...) sino que los arma
 * el service, que es quien sabe consultar las lanes.
 */
public record RolProcesoResponse(
        Long id,
        Long empresaId,
        String nombre,
        String descripcion,
        boolean activo,
        boolean enUso,
        List<String> procesosDondeSeUsa) {

    /**
     * ModelMapper solo conoce la entidad, no las lanes que usan el rol, así que el
     * servicio completa enUso y procesosDondeSeUsa después de mapear.
     */
    public RolProcesoResponse conUso(List<String> procesos) {
        return new RolProcesoResponse(id, empresaId, nombre, descripcion, activo,
                !procesos.isEmpty(), procesos);
    }

    /** Para cuando no hace falta la información de uso (por ejemplo, tras crear el rol). */
    public static RolProcesoResponse desde(RolProceso rol) {
        return desde(rol, List.of());
    }

    public static RolProcesoResponse desde(RolProceso rol, List<String> procesosDondeSeUsa) {
        return new RolProcesoResponse(
                rol.getId(),
                rol.getEmpresa().getId(),
                rol.getNombre(),
                rol.getDescripcion(),
                rol.isActivo(),
                !procesosDondeSeUsa.isEmpty(),
                procesosDondeSeUsa);
    }
}
