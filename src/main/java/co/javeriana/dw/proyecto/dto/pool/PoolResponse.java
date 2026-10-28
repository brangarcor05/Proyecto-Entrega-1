package co.javeriana.dw.proyecto.dto.pool;

import co.javeriana.dw.proyecto.entidad.Pool;

/**
 * Un participante del diagrama. Aplana el proceso a su id: devolver la entidad
 * completa arrastraria la empresa y sus colecciones, que vuelven a apuntar aqui.
 */
public record PoolResponse(
        Long id,
        Long procesoId,
        String nombre,
        boolean cajaNegra,
        boolean esPropietario,
        boolean activo) {

    /** Debe invocarse dentro de una transaccion: el proceso se carga de forma perezosa. */
    public static PoolResponse desde(Pool pool) {
        return new PoolResponse(
                pool.getId(),
                pool.getProceso().getId(),
                pool.getNombre(),
                pool.isCajaNegra(),
                pool.isEsPropietario(),
                pool.isActivo());
    }
}
