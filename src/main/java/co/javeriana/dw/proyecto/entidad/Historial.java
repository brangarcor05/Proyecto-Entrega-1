package co.javeriana.dw.proyecto.entidad;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "historial")
@Getter
@Setter
@NoArgsConstructor
public class Historial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entidad_tipo", nullable = false)
    private String entidadTipo;

    @Column(name = "entidad_id", nullable = false)
    private Long entidadId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccionHistorial accion;

    @Column(columnDefinition = "TEXT")
    private String detalle;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proceso_id")
    private Proceso proceso;
}