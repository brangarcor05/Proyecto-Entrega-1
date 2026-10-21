package co.javeriana.dw.proyecto.dto.gateway;

import co.javeriana.dw.proyecto.entidad.TipoGateway;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearGatewayRequest {
    @NotNull(message = "El ID del proceso es obligatorio")
    private Long procesoId;
    private String nombre;

    @NotBlank(message = "El tipo de gateway es obligatorio (EXCLUSIVO, PARALELO, INCLUSIVO)")
    private TipoGateway tipo; 
    private Double posicionX;
    private Double posicionY;
}