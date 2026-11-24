package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "lane",
       uniqueConstraints = @UniqueConstraint(columnNames = {"pool_id", "rol_proceso_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Lane {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pool_id", nullable = false)
    private Pool pool;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_proceso_id", nullable = false)
    private RolProceso rolProceso;
}