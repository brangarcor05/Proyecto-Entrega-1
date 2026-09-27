package co.javeriana.dw.proyecto.dto.actividad;

import lombok.Data;

@Data
public class ActividadResponse {
    private Long id;
    private Long procesoId;
    private Long laneId;
    private String nombre;
    private String tipo;
    private Double posicionX;
    private Double posicionY;
    private Boolean activo;
}