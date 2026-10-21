package co.javeriana.dw.proyecto.entidad;


import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


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

    @Column(nullable = false)
    private boolean compartido = false;

    @Column(name = "categoria", nullable = false, length = 100)
    private String categoria;

    
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProceso estado = EstadoProceso.BORRADOR;

    
    @Column(name = "activo", nullable = false)
    private boolean activo = true;
    @ManyToMany
    @JoinTable(
        name = "proceso_empresa_compartida",
        joinColumns = @JoinColumn(name = "proceso_id"),
        inverseJoinColumns = @JoinColumn(name = "empresa_id")
    )
    private Set<Empresa> empresasCompartidas = new HashSet<>();


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
