package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "evento_notificacion_externa")
@Getter
@Setter
@NoArgsConstructor
public class EventoNotificacionExterna extends NodoProceso {

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_destino", nullable = false)
    private TipoDestinoExterno tipoDestino;

    @Column(name = "momento_proceso")
    private String momentoProceso;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion_si_falla", nullable = false)
    private AccionFalloNotificacion accionSiFalla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actividad_manejo_error_id")
    private Actividad actividadManejoError; // solo si accionSiFalla = DERIVAR_ERROR

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_destino_id", nullable = false)
    private Pool poolDestino; // el sistema externo, como pool caja negra

    @ElementCollection
    @CollectionTable(name = "evento_notificacion_campo", joinColumns = @JoinColumn(name = "evento_id"))
    private List<CampoDato> campos = new ArrayList<>();
}