package co.javeriana.dw.proyecto.dto.notificacionexterna;

import java.util.List;

import co.javeriana.dw.proyecto.dto.comun.CampoDatoDto;
import co.javeriana.dw.proyecto.entidad.AccionFalloNotificacion;
import co.javeriana.dw.proyecto.entidad.TipoDestinoExterno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta de una notificacion hacia un sistema externo (correo, servicio web o cola).
 * El destino se modela como un pool caja negra: el sistema documenta el envio, no lo hace.
 */
public record CrearNotificacionExternaRequest(

        @NotNull(message = "El proceso es obligatorio")
        Long procesoId,

        @NotNull(message = "El pool es obligatorio")
        Long poolId,

        @NotNull(message = "El pool destino es obligatorio")
        Long poolDestinoId,

        @NotBlank(message = "El nombre de la notificacion es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "El tipo de destino es obligatorio")
        TipoDestinoExterno tipoDestino,

        @Size(max = 250, message = "El momento del proceso no puede superar los 250 caracteres")
        String momentoProceso,

        @NotNull(message = "La accion si falla es obligatoria")
        AccionFalloNotificacion accionSiFalla,

        /** Solo aplica cuando accionSiFalla es DERIVAR_ERROR. */
        Long actividadManejoErrorId,

        @Valid
        List<CampoDatoDto> campos,

        Double posicionX,
        Double posicionY) {
}