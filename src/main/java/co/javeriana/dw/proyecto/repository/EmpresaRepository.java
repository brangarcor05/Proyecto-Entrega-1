package co.javeriana.dw.proyecto.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.javeriana.dw.proyecto.entidad.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByRut(String rut);
    boolean existsByRut(String rut);
    boolean existsByEmail(String email);
}