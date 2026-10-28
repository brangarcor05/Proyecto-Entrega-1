package co.javeriana.dw.proyecto.repository;



import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Gateway;

public interface GatewayRepository extends JpaRepository<Gateway, Long> {
    List<Gateway> findByProcesoIdAndActivoTrue(Long procesoId);
}