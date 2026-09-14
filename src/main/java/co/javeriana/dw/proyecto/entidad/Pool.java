package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pool")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Pool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Builder.Default
    @Column(name = "caja_negra", nullable = false)
    private boolean cajaNegra = false; // true = participante externo sin elementos internos
    
    @Builder.Default
    @Column(name = "es_propietario", nullable = false)
    private boolean esPropietario = false; // true = pool de la empresa dueña del proceso

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id", nullable = false)
    private Proceso proceso;

    @OneToMany(mappedBy = "pool", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Lane> lanes = new ArrayList<>();
}