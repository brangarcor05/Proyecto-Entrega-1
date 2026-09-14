package co.javeriana.dw.proyecto.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Arco;

public interface ArcoRepository extends JpaRepository<Arco, Long> {
    List<Arco> findByProcesoIdAndActivoTrue(Long procesoId);

    // para HU-10/HU-13: encontrar arcos colgando de un nodo antes de borrarlo
    List<Arco> findByOrigenIdOrDestinoId(Long origenId, Long destinoId);

    boolean existsByOrigenIdAndDestinoId(Long origenId, Long destinoId);

    // para detectar "elemento sin camino de entrada/salida" al eliminar un arco
    boolean existsByOrigenId(Long nodoId);
    boolean existsByDestinoId(Long nodoId);
}