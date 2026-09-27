package co.javeriana.dw.proyecto.dto.gateway;

import co.javeriana.dw.proyecto.entidad.TipoGateway;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarGatewayRequest {
    private String nombre;

    @NotBlank(message = "El tipo de gateway es obligatorio")
    private TipoGateway tipo;
    private Double posicionX;
    private Double posicionY;
}