package co.javeriana.dw.proyecto.dto.gateway;

import co.javeriana.dw.proyecto.entidad.TipoGateway;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarGatewayRequest {
    private String nombre;

    @NotNull(message = "El tipo de gateway es obligatorio")
    private TipoGateway tipo;

    private Long poolId;
    private Double posicionX;
    private Double posicionY;
    private String canvasId;
}