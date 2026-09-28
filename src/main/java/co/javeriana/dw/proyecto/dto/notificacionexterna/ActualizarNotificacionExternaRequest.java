package co.javeriana.dw.proyecto.dto.notificacionexterna;

import java.util.List;

import co.javeriana.dw.proyecto.dto.comun.CampoDatoDto;
import co.javeriana.dw.proyecto.entidad.AccionFalloNotificacion;
import co.javeriana.dw.proyecto.entidad.TipoDestinoExterno;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos editables de una notificacion externa. No incluye proceso ni pool contenedor. */
public record ActualizarNotificacionExternaRequest(

        @NotBlank(message = "El nombre de la notificacion es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotNull(message = "El pool destino es obligatorio")
        Long poolDestinoId,

        @NotNull(message = "El tipo de destino es obligatorio")
        TipoDestinoExterno tipoDestino,

        @Size(max = 250, message = "El momento del proceso no puede superar los 250 caracteres")
        String momentoProceso,

        @NotNull(message = "La accion si falla es obligatoria")
        AccionFalloNotificacion accionSiFalla,

        Long actividadManejoErrorId,

        @Valid
        List<CampoDatoDto> campos,

        Double posicionX,
        Double posicionY) {
}