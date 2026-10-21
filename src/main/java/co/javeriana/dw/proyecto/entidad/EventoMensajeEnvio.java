package co.javeriana.dw.proyecto.entidad;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "evento_mensaje_envio")
@Getter
@Setter
@NoArgsConstructor
public class EventoMensajeEnvio extends NodoProceso {

    @Column(name = "nombre_mensaje", nullable = false)
    private String nombreMensaje;

    @Column(name = "clave_correlacion")
    private String claveCorrelacion; // HU-28

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_destino_id", nullable = false)
    private Pool poolDestino;

    @ElementCollection
    @CollectionTable(name = "evento_mensaje_envio_campo", joinColumns = @JoinColumn(name = "evento_id"))
    private List<CampoDato> campos = new ArrayList<>();
}
