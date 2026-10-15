package co.javeriana.dw.proyecto.dto.mensaje;

import co.javeriana.dw.proyecto.entidad.VarianteMensajeCatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CrearEventoMensajeRecepcionRequest {

    @NotNull(message = "El ID del proceso es obligatorio")
    private Long procesoId;

    @NotNull(message = "El ID del pool que contiene el evento es obligatorio")
    private Long poolId;

    @NotBlank(message = "El nombre del mensaje es obligatorio")
    private String nombreMensaje;

    @NotNull(message = "La variante del mensaje es obligatoria")
    private VarianteMensajeCatch variante;

    private String datosEsperados;
    private boolean origenExterno;
    private String claveCorrelacion;

    private Double posicionX;
    private Double posicionY;
}
