package co.javeriana.dw.proyecto.dto.actividad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ActualizarActividadRequest {
    @NotBlank(message = "El nombre de la actividad es obligatorio")
    private String nombre;

    @NotBlank(message = "El tipo de actividad es obligatorio")
    private String tipo;

    @NotNull(message = "El ID de la lane es obligatorio")
    private Long laneId;
    private Double posicionX;
    private Double posicionY;
}