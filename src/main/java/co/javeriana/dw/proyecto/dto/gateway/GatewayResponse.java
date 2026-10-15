package co.javeriana.dw.proyecto.dto.gateway;

import co.javeriana.dw.proyecto.entidad.TipoGateway;
import lombok.Data;

@Data
public class GatewayResponse {
    private Long id;
    private Long procesoId;
    private Long poolId;
    private String nombre;
    private TipoGateway tipo;
    private Double posicionX;
    private Double posicionY;
    private Boolean activo;
}
