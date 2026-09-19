package co.javeriana.dw.proyecto.dto.proceso;

import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;

/**
 * Informacion de un proceso que devuelve la API.
 * Aplana la empresa a id y nombre: devolver la entidad Empresa completa
 * provoca recursion infinita al serializar, porque Empresa vuelve a listar sus procesos.
 */
public record ProcesoResponse(
        Long id,
        Long empresaId,
        String empresaNombre,
        String nombre,
        String descripcion,
        String categoria,
        EstadoProceso estado,
        boolean activo,
        boolean compartido,
        /** HU-23: el proceso llega por comparticion, la empresa invitada no puede editarlo. */
        boolean soloLectura) {

    /**
     * Debe invocarse dentro de una transaccion: la empresa se carga de forma perezosa.
     *
     * @param empresaIdConsultante empresa que hace la peticion, para saber si el proceso
     *                             es propio o llega compartido desde otra organizacion.
     */
    public static ProcesoResponse desde(Proceso proceso, Long empresaIdConsultante) {
        boolean esPropio = proceso.getEmpresa().getId().equals(empresaIdConsultante);
        return new ProcesoResponse(
                proceso.getId(),
                proceso.getEmpresa().getId(),
                proceso.getEmpresa().getNombre(),
                proceso.getNombre(),
                proceso.getDescripcion(),
                proceso.getCategoria(),
                proceso.getEstado(),
                proceso.isActivo(),
                proceso.isCompartido(),
                !esPropio);
    }
}
