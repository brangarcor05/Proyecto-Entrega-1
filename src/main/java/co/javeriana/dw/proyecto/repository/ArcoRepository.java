package co.javeriana.dw.proyecto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Arco;

public interface ArcoRepository extends JpaRepository<Arco, Long> {

    List<Arco> findByProcesoIdAndActivoTrue(Long procesoId);

    Optional<Arco> findByIdAndActivoTrue(Long id);

    // para HU-10/HU-13: encontrar arcos colgando de un nodo antes de borrarlo
    List<Arco> findByOrigenIdOrDestinoId(Long origenId, Long destinoId);

    /*
     La restriccion unica (origen_id, destino_id) es de la tabla y no distingue
     arcos inactivos, asi que al recrear un arco borrado hay que reactivar la fila
     existente en vez de insertar otra.
     */
    Optional<Arco> findByOrigenIdAndDestinoId(Long origenId, Long destinoId);

    boolean existsByOrigenIdAndDestinoIdAndActivoTrue(Long origenId, Long destinoId);

    // para detectar "elemento sin camino de entrada/salida" al eliminar un arco
    boolean existsByOrigenIdAndActivoTrue(Long nodoId);

    boolean existsByDestinoIdAndActivoTrue(Long nodoId);
}
