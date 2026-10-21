package co.javeriana.dw.proyecto.dto.notificacionexterna;

import java.util.List;

import co.javeriana.dw.proyecto.dto.comun.CampoDatoDto;
import co.javeriana.dw.proyecto.entidad.AccionFalloNotificacion;
import co.javeriana.dw.proyecto.entidad.EventoNotificacionExterna;
import co.javeriana.dw.proyecto.entidad.TipoDestinoExterno;

/** Aplana proceso, pools y actividad de error a sus ids. */
public record NotificacionExternaResponse(
        Long id,
        Long procesoId,
        Long poolId,
        Long poolDestinoId,
        String nombre,
        TipoDestinoExterno tipoDestino,
        String momentoProceso,
        AccionFalloNotificacion accionSiFalla,
        Long actividadManejoErrorId,
        List<CampoDatoDto> campos,
        Double posicionX,
        Double posicionY,
        boolean activo) {

    /** Debe invocarse dentro de una transaccion: las relaciones se cargan de forma perezosa. */
    public static NotificacionExternaResponse desde(EventoNotificacionExterna notificacion) {
        return new NotificacionExternaResponse(
                notificacion.getId(),
                notificacion.getProceso().getId(),
                notificacion.getPool().getId(),
                notificacion.getPoolDestino().getId(),
                notificacion.getNombre(),
                notificacion.getTipoDestino(),
                notificacion.getMomentoProceso(),
                notificacion.getAccionSiFalla(),
                notificacion.getActividadManejoError() == null
                        ? null
                        : notificacion.getActividadManejoError().getId(),
                notificacion.getCampos().stream().map(CampoDatoDto::desde).toList(),
                notificacion.getPosicionX(),
                notificacion.getPosicionY(),
                notificacion.isActivo());
    }
}