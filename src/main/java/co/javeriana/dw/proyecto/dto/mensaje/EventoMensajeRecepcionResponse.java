package co.javeriana.dw.proyecto.dto.mensaje;

import co.javeriana.dw.proyecto.entidad.VarianteMensajeCatch;
import lombok.Data;

@Data
public class EventoMensajeRecepcionResponse {
    private Long id;
    private Long procesoId;
    private String nombreMensaje;
    private VarianteMensajeCatch variante;
    private String datosEsperados;
    private boolean origenExterno;
    private String claveCorrelacion;
    private Double posicionX;
    private Double posicionY;
    private Boolean activo;
}