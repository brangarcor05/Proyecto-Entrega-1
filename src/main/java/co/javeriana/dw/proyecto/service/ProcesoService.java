package co.javeriana.dw.proyecto.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class ProcesoService {
    private final ProcesoRepository procesoRepository;
    private final EmpresaRepository empresaRepository;

    public ProcesoService(ProcesoRepository procesoRepository, EmpresaRepository empresaRepository) {
        this.procesoRepository = procesoRepository;
        this.empresaRepository = empresaRepository;
    }

    @Transactional
    public ProcesoResponse crear(CrearProcesoRequest request) {
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la empresa " + request.empresaId()));

        if (procesoRepository.existsByEmpresaAndNombre(empresa, request.nombre())) {
            throw new NombreDuplicadoException(
                    "Ya existe un proceso llamado '" + request.nombre() + "' en esta empresa.");
        }

        Proceso proceso = new Proceso(empresa, request.nombre(), request.descripcion(), request.categoria());
        return ProcesoResponse.desde(procesoRepository.save(proceso));
    }

    @Transactional
    public ProcesoResponse actualizar(Long id, ActualizarProcesoRequest request) {
        Proceso proceso = obtenerActivo(id);

        boolean cambiaNombre = !proceso.getNombre().equals(request.nombre());
        if (cambiaNombre && procesoRepository.existsByEmpresaAndNombre(proceso.getEmpresa(), request.nombre())) {
            throw new NombreDuplicadoException(
                    "Ya existe un proceso llamado '" + request.nombre() + "' en esta empresa.");
        }

        proceso.setNombre(request.nombre());
        proceso.setDescripcion(request.descripcion());
        proceso.setCategoria(request.categoria());
        proceso.setEstado(request.estado());
        return ProcesoResponse.desde(procesoRepository.save(proceso));
    }

    @Transactional(readOnly = true)
    public ProcesoResponse obtener(Long id) {
        return ProcesoResponse.desde(obtenerActivo(id));
    }

    /**
     * Listado de HU-07: siempre acotado a una empresa, con búsqueda por nombre
     * y filtros combinables de estado y categoría. Los procesos eliminados
     * quedan fuera salvo que se pidan explícitamente.
     */
    @Transactional(readOnly = true)
    public Page<ProcesoResponse> consultar(Long empresaId, String nombre, EstadoProceso estado,
                                           String categoria, boolean incluirInactivos, Pageable pageable) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new RecursoNoEncontradoException("No existe la empresa " + empresaId);
        }

        Specification<Proceso> filtro = (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(root.get("empresa").get("id"), empresaId));
            if (!incluirInactivos) {
                condiciones.add(cb.isTrue(root.get("activo")));
            }
            if (nombre != null && !nombre.isBlank()) {
                condiciones.add(cb.like(cb.lower(root.get("nombre")),
                        "%" + nombre.toLowerCase() + "%"));
            }
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (categoria != null && !categoria.isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("categoria")), categoria.toLowerCase()));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };

        return procesoRepository.findAll(filtro, pageable).map(ProcesoResponse::desde);
    }

    /** Eliminación lógica de HU-06: el proceso se conserva y deja de listarse. */
    @Transactional
    public void eliminar(Long id) {
        Proceso proceso = obtenerActivo(id);
        proceso.setActivo(false);
        procesoRepository.save(proceso);
    }

    private Proceso obtenerActivo(Long id) {
        return procesoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el proceso " + id));
    }

    /* ------------------------------------------------------------------
       Consultas sobre entidades, para uso interno de otros servicios
       ------------------------------------------------------------------ */

    public Proceso guardar(Proceso proceso) {
        Optional<Proceso> procesoExistente = procesoRepository.findByEmpresaAndNombre(proceso.getEmpresa(), proceso.getNombre());
        if (procesoExistente.isPresent() && !procesoExistente.get().getId().equals(proceso.getId())) {
            throw new NombreDuplicadoException("Ya existe un proceso con ese nombre en la empresa.");
        }
        return procesoRepository.save(proceso);
    }

    public List<Proceso> listarTodos() {
        return procesoRepository.findAllByOrderByIdAsc();
    }

    public Optional<Proceso> buscarPorId(Long id) {
        return procesoRepository.findById(id);
    }

    public List<Proceso> listarPorEmpresa(Empresa empresa) {
        return procesoRepository.findByEmpresaAndActivo(empresa, true);
    }

    public List<Proceso> listarPorEmpresaYEstado(Empresa empresa, EstadoProceso estado) {
        return procesoRepository.findByEmpresaAndActivoAndEstado(empresa, true, estado);
    }

    public List<Proceso> listarPorEmpresaYCategoria(Empresa empresa, String categoria) {
        return procesoRepository.findByEmpresaAndActivoAndCategoria(empresa, true, categoria);
    }

    public List<Proceso> listarPorEstado(Empresa empresa, EstadoProceso estado) {
        return procesoRepository.findByEmpresaAndEstado(empresa, estado);
    }

    public List<Proceso> listarPorCategoria(Empresa empresa, String categoria) {
        return procesoRepository.findByEmpresaAndCategoria(empresa, categoria);
    }
}
