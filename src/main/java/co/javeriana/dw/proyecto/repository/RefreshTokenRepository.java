package co.javeriana.dw.proyecto.repository;

import co.javeriana.dw.proyecto.entidad.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Cierra todas las sesiones abiertas de un usuario (desactivación o robo detectado). */
    @Modifying(flushAutomatically = true)
    @Query("""
           UPDATE RefreshToken r
              SET r.revocado = true, r.fechaRevocacion = :ahora, r.reemplazado = false
            WHERE r.usuario.id = :usuarioId AND r.revocado = false
           """)
    int revocarTodosDelUsuario(@Param("usuarioId") Long usuarioId, @Param("ahora") Instant ahora);

    /** Limpieza: los tokens vencidos ya no sirven para nada. */
    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM RefreshToken r WHERE r.usuario.id = :usuarioId AND r.fechaExpiracion < :limite")
    int eliminarVencidosDelUsuario(@Param("usuarioId") Long usuarioId, @Param("limite") Instant limite);
}
