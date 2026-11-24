package co.javeriana.dw.proyecto.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cuerpo uniforme de error de la API.
 * {@code camposInvalidos} solo viaja cuando falla la validación de un formulario (HU-04).
 */
public record ErrorResponse(
        int estado,
        String error,
        String mensaje,
        Map<String, String> camposInvalidos,
        LocalDateTime momento) {

    public static ErrorResponse de(int estado, String error, String mensaje) {
        return new ErrorResponse(estado, error, mensaje, null, LocalDateTime.now());
    }

    public static ErrorResponse deValidacion(int estado, String error, String mensaje,
                                             Map<String, String> camposInvalidos) {
        return new ErrorResponse(estado, error, mensaje, camposInvalidos, LocalDateTime.now());
    }
}
