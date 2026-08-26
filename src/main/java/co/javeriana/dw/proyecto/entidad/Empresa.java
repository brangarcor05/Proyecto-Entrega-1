package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "empresas",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_empresa_runt", columnNames = "runt"),
           @UniqueConstraint(name = "uk_empresa_email", columnNames = "email")
       })
@SQLDelete(sql = "UPDATE empresas SET estado = 'INACTIVO' WHERE id = ?")
@SQLRestriction(value = "estado != 'INACTIVO'")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 20, unique = true)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(length = 20)
    private String telefono;

    @Column(length = 255)
    private String direccion;

    @Column(length = 50)
    private String sector;

    @Column(length = 20)
    @Builder.Default
    private String estado = "ACTIVO";

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "admin_usuario_id")
    private Long adminUsuarioId;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Usuario> usuarios = new ArrayList<>();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Proceso> procesos = new ArrayList<>();

    public boolean isActiva() {
        return "ACTIVO".equals(this.estado);
    }
}