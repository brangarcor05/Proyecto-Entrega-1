package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

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
     Constructor vacío requerido por JPA.
     */
    public Proceso() {
    }

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public void setEmpresa(Empresa empresa) {
        this.empresa = empresa;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public EstadoProceso getEstado() {
        return estado;
    }

    public void setEstado(EstadoProceso estado) {
        this.estado = estado;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
