package co.javeriana.dw.proyecto.dto.mensaje;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarEventoMensajeEnvioRequest {

    @NotBlank(message = "El nombre del mensaje es obligatorio")
    private String nombreMensaje;

    private String claveCorrelacion;

    @NotNull(message = "El ID del pool destino es obligatorio")
    private Long poolDestinoId;

    private Double posicionX;
    private Double posicionY;
}