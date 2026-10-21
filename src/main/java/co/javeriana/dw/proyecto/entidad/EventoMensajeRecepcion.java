package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_mensaje_recepcion")
@Getter
@Setter
@NoArgsConstructor
public class EventoMensajeRecepcion extends NodoProceso {

    @Column(name = "nombre_mensaje", nullable = false)
    private String nombreMensaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VarianteMensajeCatch variante;

    @Column(name = "datos_esperados", columnDefinition = "TEXT")
    private String datosEsperados;

    @Column(name = "origen_externo", nullable = false)
    private boolean origenExterno = false; 

    @Column(name = "clave_correlacion")
    private String claveCorrelacion; 
}