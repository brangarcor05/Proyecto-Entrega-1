package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Token de larga duración (refresh token) de HU-03.
 *
 * El access token (JWT) dura minutos y no se guarda en ningún lado: se valida solo con la
 * firma. El refresh token dura días, así que tiene que poder revocarse (logout, usuario
 * desactivado, robo detectado): por eso vive en la base de datos.
 *
 * Nunca se guarda el token en claro, solo su hash SHA-256. Si alguien lee la tabla no
 * puede usar lo que encuentra, igual que con las contraseñas.
 */
@Entity
@Table(name = "refresh_tokens",
       indexes = @Index(name = "idx_refresh_token_usuario", columnList = "usuario_id"))
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Usuario usuario;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private Instant fechaExpiracion;

    @Column(nullable = false)
    private boolean revocado = false;

    @Column(name = "fecha_revocacion")
    private Instant fechaRevocacion;

    /**
     * true cuando se revocó porque se cambió por uno nuevo (rotación), false cuando se
     * revocó por logout, desactivación o sospecha de robo. Sirve para distinguir una
     * carrera inocente entre dos pestañas de una reutilización real.
     */
    @Column(nullable = false)
    private boolean reemplazado = false;

    public boolean estaVencido(Instant ahora) {
        return !fechaExpiracion.isAfter(ahora);
    }

    public void revocar(Instant ahora, boolean porRotacion) {
        this.revocado = true;
        this.fechaRevocacion = ahora;
        this.reemplazado = porRotacion;
    }
}
