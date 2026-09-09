package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "actividad")
@Getter
@Setter
@NoArgsConstructor
public class Actividad extends NodoProceso {

    @Column(nullable = false)
    private String tipo; 

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_proceso_id", nullable = false)
    private RolProceso rolProceso; // la lane: define el responsable
}