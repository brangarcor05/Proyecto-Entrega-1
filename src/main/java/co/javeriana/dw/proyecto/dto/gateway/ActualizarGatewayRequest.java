package co.javeriana.dw.proyecto.dto.gateway;

import co.javeriana.dw.proyecto.entidad.TipoGateway;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarGatewayRequest {
    @NotBlank(message = "El nombre del gateway es obligatorio")
    private String nombre;

    @NotNull(message = "El tipo de gateway es obligatorio")
    private TipoGateway tipo;
    private Double posicionX;
    private Double posicionY;
}
