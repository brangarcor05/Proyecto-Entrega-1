package co.javeriana.dw.proyecto.repository;

import co.javeriana.dw.proyecto.entidad.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {

    List<Empresa> findAllByOrderByIdAsc();

<<<<<<< HEAD
}

=======
>>>>>>> 05e26fa566eab0cb8c63197e7bc337b389f85462
