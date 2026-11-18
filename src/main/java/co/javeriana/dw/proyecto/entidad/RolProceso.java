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

/**
 * Rol de proceso (HU-17 a HU-20): el catalogo de funciones de la empresa (Analista,
 * Supervisor, Auditor...) que se usa para nombrar las lanes de sus diagramas. No debe
 * confundirse con RolUsuario, el rol de acceso a la aplicacion (HU-02).
 */
@Entity
@Table(name = "rol_proceso",
       uniqueConstraints = @UniqueConstraint(name = "uk_rolproceso_empresa_nombre",
               columnNames = {"empresa_id", "nombre"}))
@Getter
@Setter
@NoArgsConstructor
public class RolProceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // Opcional: los tests que ya existen (PoolReglasTest, ArcoReglasTest) crean roles
    // de apoyo sin describirlos, y HU-17 no exige el campo como obligatorio.
    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
}
