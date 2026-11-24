package co.javeriana.dw.proyecto.dto.permisopool;

import co.javeriana.dw.proyecto.entidad.PermisoPool;
import co.javeriana.dw.proyecto.entidad.RolUsuario;

/**
 * Permisos de un rol de acceso sobre los pools y lanes de la empresa (HU-24).
 *
 * {@code configurado} distingue lo que la empresa definio explicitamente de los
 * valores por defecto que aplica el sistema mientras nadie los cambia.
 */
public record PermisoPoolResponse(
        Long id,
        Long empresaId,
        RolUsuario rolUsuario,
        boolean puedeCrear,
        boolean puedeEditar,
        boolean puedeEliminar,
        boolean configurado) {

    /** Debe invocarse dentro de una transaccion: la empresa se carga de forma perezosa. */
    public static PermisoPoolResponse desde(PermisoPool permiso) {
        return new PermisoPoolResponse(
                permiso.getId(),
                permiso.getEmpresa().getId(),
                permiso.getRolUsuario(),
                permiso.isPuedeCrear(),
                permiso.isPuedeEditar(),
                permiso.isPuedeEliminar(),
                true);
    }

    /** Valores que rigen cuando la empresa todavia no configuro ese rol. */
    public static PermisoPoolResponse porDefecto(Long empresaId, RolUsuario rolUsuario) {
        boolean esAdmin = rolUsuario == RolUsuario.ADMIN;
        boolean esEditor = rolUsuario == RolUsuario.EDITOR;
        return new PermisoPoolResponse(
                null, empresaId, rolUsuario,
                esAdmin || esEditor,
                esAdmin || esEditor,
                esAdmin,
                false);
    }
}
