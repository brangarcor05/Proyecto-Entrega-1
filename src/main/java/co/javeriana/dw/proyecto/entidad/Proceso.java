package co.javeriana.dw.proyecto.entidad;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;


@Entity
@Table(
    name = "procesos",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_proceso_empresa_nombre",
            columnNames = {"empresa_id", "nombre"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Proceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     Muchos procesos pueden pertenecer a una empresa.
     Cada proceso pertenece únicamente a una empresa.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    /*
     El nombre debe ser único dentro de cada empresa.
     */
    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", nullable = false, length = 2000)
    private String descripcion;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    /*
     Un proceso nuevo comienza en estado BORRADOR.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProceso estado = EstadoProceso.BORRADOR;

    /*
     Se utiliza para la eliminación lógica.
     false significa que el proceso fue eliminado.
     */
    @Column(name = "activo", nullable = false)
    private boolean activo = true;


    /*
     Constructor para crear un proceso nuevo.
     El estado y activo no se reciben porque todo proceso
     debe comenzar como BORRADOR y activo.
     */
    public Proceso(
            Empresa empresa,
            String nombre,
            String descripcion,
            String categoria) {
        this.empresa = empresa;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.estado = EstadoProceso.BORRADOR;
        this.activo = true;
    }

}
