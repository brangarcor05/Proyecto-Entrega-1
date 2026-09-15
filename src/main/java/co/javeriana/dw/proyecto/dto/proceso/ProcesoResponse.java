package co.javeriana.dw.proyecto.dto.proceso;

import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;

/**
 * Información de un proceso que devuelve la API.
 * Aplana la empresa a id y nombre: devolver la entidad Empresa completa
 * provoca recursión infinita al serializar, porque Empresa vuelve a listar sus procesos.
 */
public record ProcesoResponse(
        Long id,
        Long empresaId,
        String empresaNombre,
        String nombre,
        String descripcion,
        String categoria,
        EstadoProceso estado,
        boolean activo) {

    /** Debe invocarse dentro de una transacción: la empresa se carga de forma perezosa. */
    public static ProcesoResponse desde(Proceso proceso) {
        return new ProcesoResponse(
                proceso.getId(),
                proceso.getEmpresa().getId(),
                proceso.getEmpresa().getNombre(),
                proceso.getNombre(),
                proceso.getDescripcion(),
                proceso.getCategoria(),
                proceso.getEstado(),
                proceso.isActivo());
    }
}
