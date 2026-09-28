package co.javeriana.dw.proyecto.dto.mensaje;

import lombok.Data;

@Data
public class EventoMensajeEnvioResponse {
    private Long id;
    private Long procesoId;
    private Long poolId;
    private String nombreMensaje;
    private String claveCorrelacion;
    private Long poolDestinoId;
    private String nombrePoolDestino;
    private Double posicionX;
    private Double posicionY;
    private Boolean activo;
}
