package co.javeriana.dw.proyecto.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.historial.HistorialResponse;
import co.javeriana.dw.proyecto.dto.proceso.ActualizarProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.CrearProcesoRequest;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class ProcesoService {

    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialService historialService;

    public ProcesoService(ProcesoRepository procesoRepository, PoolRepository poolRepository,
                          EmpresaRepository empresaRepository, UsuarioRepository usuarioRepository,
                          HistorialService historialService) {
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialService = historialService;
    }

    @Transactional
    public ProcesoResponse crear(CrearProcesoRequest request, Long usuarioId) {
        Usuario usuarioCreador = obtenerUsuario(usuarioId);
        validarPuedeEditar(usuarioCreador);

        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la empresa " + request.empresaId()));

        if (procesoRepository.existsByNombreAndEmpresaId(request.nombre(), empresa.getId())) {
            throw new NombreDuplicadoException(
                    "Ya existe un proceso con ese nombre en esta empresa");
        }

        Proceso proceso = new Proceso();
        proceso.setNombre(request.nombre());
        proceso.setDescripcion(request.descripcion());
        proceso.setCategoria(request.categoria());
        proceso.setEstado(EstadoProceso.BORRADOR);
        proceso.setEmpresa(empresa);
        proceso = procesoRepository.save(proceso);

        // HU-04: "al crearlo queda listo para agregarle elementos, con el pool de la
        // empresa ya definido" - se crea el pool propietario automaticamente.
        Pool poolPropietario = new Pool();
        poolPropietario.setNombre(empresa.getNombre());
        poolPropietario.setEsPropietario(true);
        poolPropietario.setCajaNegra(false);
        poolPropietario.setProceso(proceso);
        poolRepository.save(poolPropietario);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.CREACION,
                usuarioCreador, proceso, "Proceso creado");
        return ProcesoResponse.desde(proceso);
    }

    @Transactional
    public ProcesoResponse actualizar(Long procesoId, ActualizarProcesoRequest request, Long usuarioId) {
        Usuario usuarioEditor = obtenerUsuario(usuarioId);
        validarPuedeEditar(usuarioEditor);
        Proceso proceso = obtenerActivo(procesoId);

        boolean cambioNombre = !proceso.getNombre().equals(request.nombre());
        if (cambioNombre
                && procesoRepository.existsByNombreAndEmpresaId(request.nombre(), proceso.getEmpresa().getId())) {
            throw new NombreDuplicadoException(
                    "Ya existe un proceso con ese nombre en esta empresa");
        }

        proceso.setNombre(request.nombre());
        proceso.setDescripcion(request.descripcion());
        proceso.setCategoria(request.categoria());
        proceso.setEstado(request.estado());
        proceso = procesoRepository.save(proceso);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.EDICION,
                usuarioEditor, proceso, "Datos del proceso actualizados");
        return ProcesoResponse.desde(proceso);
    }

    @Transactional(readOnly = true)
    public ProcesoResponse obtener(Long procesoId) {
        return ProcesoResponse.desde(obtenerActivo(procesoId));
    }

    /** HU-07: historial de cambios del proceso. Se valida que el proceso exista. */
    @Transactional(readOnly = true)
    public List<HistorialResponse> consultarHistorial(Long procesoId) {
        if (!procesoRepository.existsById(procesoId)) {
            throw new RecursoNoEncontradoException("Proceso no encontrado: " + procesoId);
        }
        return historialService.listarPorProceso(procesoId);
    }

    /**
     * Listado de HU-07: acotado a una empresa, con busqueda por nombre y filtros
     * combinables de estado y categoria. Los tres se aplican a la vez cuando se envian
     * juntos; los procesos eliminados quedan fuera salvo que se pidan explicitamente.
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
                condiciones.add(cb.notEqual(root.get("estado"), EstadoProceso.INACTIVO));
            }
            if (nombre != null && !nombre.isBlank()) {
                condiciones.add(cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%"));
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

    /** Eliminacion logica de HU-06: el proceso se conserva y deja de listarse. */
    @Transactional
    public void eliminar(Long procesoId, Long usuarioId) {
        Usuario usuarioAdministrador = obtenerUsuario(usuarioId);
        if (usuarioAdministrador.getRolUsuario() != RolUsuario.ADMIN) {
            throw new PermisoDenegadoException("Solo un administrador puede eliminar procesos");
        }
        Proceso proceso = obtenerActivo(procesoId);
        // Se marcan los dos campos para que no queden contradictorios: el estado es el
        // que consultan los filtros, y activo ya venia en la entidad desde antes.
        proceso.setEstado(EstadoProceso.INACTIVO);
        proceso.setActivo(false);
        procesoRepository.save(proceso);

        historialService.registrar("Proceso", proceso.getId(), AccionHistorial.ELIMINACION,
                usuarioAdministrador, proceso, "Proceso marcado como inactivo");
    }

    /** HU-05: "Solo usuarios con rol administrador o editor pueden modificar". */
    private void validarPuedeEditar(Usuario usuario) {
        boolean puedeEditar = usuario.getRolUsuario() == RolUsuario.ADMIN
                || usuario.getRolUsuario() == RolUsuario.EDITOR;
        if (!puedeEditar) {
            throw new PermisoDenegadoException("Los usuarios de solo lectura no pueden editar procesos");
        }
    }

    private Proceso obtenerActivo(Long id) {
        return procesoRepository.findByIdAndEstadoNot(id, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
    }
}
