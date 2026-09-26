package co.javeriana.dw.proyecto.dto.evento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos editables de un evento. No incluye proceso ni pool: un evento no cambia de diagrama. */
public record ActualizarEventoRequest(

        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar los 150 caracteres")
        String nombre,

        @NotBlank(message = "El tipo del evento es obligatorio")
        @Pattern(regexp = "INICIO|FIN", message = "El tipo debe ser INICIO o FIN")
        String tipo,

        Double posicionX,
        Double posicionY) {
}
