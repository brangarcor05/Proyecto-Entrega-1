package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.rolproceso.ActualizarRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.CrearRolProcesoRequest;
import co.javeriana.dw.proyecto.dto.rolproceso.RolProcesoResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolProceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.RolProcesoRepository;

/**
 * Roles de proceso (HU-17 a HU-20): el catálogo de funciones de la empresa (Analista,
 * Supervisor, Auditor...) que se reutiliza para nombrar lanes en cualquier proceso.
 * No confundir con RolUsuario (HU-02), que es el rol de acceso a la aplicación.
 *
 * A diferencia de Pool y Lane, aquí no aplican los permisos configurables de HU-24:
 * esa historia solo cubre pools y lanes, así que crear/editar/eliminar el catálogo
 * de roles queda con la regla explícita de HU-17 y HU-19: solo el administrador.
 */
@Service
public class RolProcesoService {

    private final RolProcesoRepository rolProcesoRepository;
    private final EmpresaRepository empresaRepository;
    private final LaneRepository laneRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;

    public RolProcesoService(RolProcesoRepository rolProcesoRepository, EmpresaRepository empresaRepository,
                             LaneRepository laneRepository, PermisoService permisoService,
                             HistorialService historialService) {
        this.rolProcesoRepository = rolProcesoRepository;
        this.empresaRepository = empresaRepository;
        this.laneRepository = laneRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
    }

    /** HU-17: "Solo el administrador de la empresa puede crear roles." */
    @Transactional
    public RolProcesoResponse crear(CrearRolProcesoRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        if (!usuario.getEmpresa().getId().equals(request.empresaId())) {
            throw new PermisoDenegadoException(
                    "No se pueden crear roles para una empresa distinta a la del usuario");
        }
        Empresa empresa = empresaRepository.findById(request.empresaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa " + request.empresaId()));

        if (rolProcesoRepository.existsByNombreIgnoreCaseAndEmpresaId(request.nombre(), empresa.getId())) {
            throw new NombreDuplicadoException(
                    "Ya existe un rol de proceso con ese nombre en esta empresa");
        }

        RolProceso rol = new RolProceso();
        rol.setNombre(request.nombre());
        rol.setDescripcion(request.descripcion());
        rol.setActivo(true);
        rol.setEmpresa(empresa);
        rol = rolProcesoRepository.save(rol);

        historialService.registrar("RolProceso", rol.getId(), AccionHistorial.CREACION,
                usuario, null, "Rol de proceso creado: " + rol.getNombre());
        return RolProcesoResponse.desde(rol);
    }

    /**
     * HU-18. El nombre y la descripción son los únicos datos editables. Renombrar el
     * rol no toca ninguna lane: las lanes solo guardan la referencia al rol (su
     * "etiqueta" en el diagrama sale de rol.getNombre() en el momento de consultarlas),
     * así que todas quedan al día automáticamente.
     */
    @Transactional
    public RolProcesoResponse actualizar(Long rolId, ActualizarRolProcesoRequest request, Long usuarioId) {
        RolProceso rol = obtenerActivo(rolId);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, rol);

        if (rolProcesoRepository.existsByNombreIgnoreCaseAndEmpresaIdAndIdNot(
                request.nombre(), rol.getEmpresa().getId(), rol.getId())) {
            throw new NombreDuplicadoException(
                    "Ya existe un rol de proceso con ese nombre en esta empresa");
        }

        rol.setNombre(request.nombre());
        rol.setDescripcion(request.descripcion());
        rol = rolProcesoRepository.save(rol);

        historialService.registrar("RolProceso", rol.getId(), AccionHistorial.EDICION,
                usuario, null, "Rol de proceso actualizado: " + rol.getNombre());
        return RolProcesoResponse.desde(rol, procesosDondeSeUsa(rol.getId()));
    }

    @Transactional(readOnly = true)
    public RolProcesoResponse obtener(Long rolId, Long empresaId) {
        RolProceso rol = obtenerActivo(rolId);
        if (!rol.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoNoEncontradoException("Rol de proceso no encontrado: " + rolId);
        }
        return RolProcesoResponse.desde(rol, procesosDondeSeUsa(rol.getId()));
    }

    /**
     * HU-20: listado paginado y con búsqueda por nombre, solo de la empresa del
     * usuario autenticado, marcando para cada rol si está en uso y en qué procesos.
     */
    @Transactional(readOnly = true)
    public Page<RolProcesoResponse> consultar(Long empresaId, String nombre, Pageable pageable) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new RecursoNoEncontradoException("No existe la empresa " + empresaId);
        }
        Page<RolProceso> pagina = (nombre == null || nombre.isBlank())
                ? rolProcesoRepository.findByEmpresaIdAndActivoTrue(empresaId, pageable)
                : rolProcesoRepository.findByEmpresaIdAndActivoTrueAndNombreContainingIgnoreCase(
                        empresaId, nombre, pageable);

        // N+1 a propósito: el catálogo de roles de una empresa es chico (decenas, no
        // miles), así que no compensa complicar la consulta con una agregación batch.
        return pagina.map(rol -> RolProcesoResponse.desde(rol, procesosDondeSeUsa(rol.getId())));
    }

    /**
     * HU-19: eliminación lógica, solo del administrador, y bloqueada si el rol está
     * en uso en alguna lane. En vez de un rechazo genérico, HU-19 pide indicar en qué
     * procesos está siendo usado.
     */
    @Transactional
    public void eliminar(Long rolId, Long usuarioId) {
        RolProceso rol = obtenerActivo(rolId);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, rol);

        List<String> procesos = procesosDondeSeUsa(rol.getId());
        if (!procesos.isEmpty()) {
            throw new ReglaNegocioException(
                    "El rol '" + rol.getNombre() + "' está en uso y no se puede eliminar. "
                            + "Reasigne primero las lanes en: " + String.join(", ", procesos));
        }

        rol.setActivo(false);
        rolProcesoRepository.save(rol);

        historialService.registrar("RolProceso", rol.getId(), AccionHistorial.ELIMINACION,
                usuario, null, "Rol de proceso marcado como inactivo: " + rol.getNombre());
    }

    private List<String> procesosDondeSeUsa(Long rolId) {
        return laneRepository.nombresDeProcesosQueUsanElRol(rolId);
    }

    private void validarEsDeLaEmpresa(Usuario usuario, RolProceso rol) {
        if (!usuario.getEmpresa().getId().equals(rol.getEmpresa().getId())) {
            throw new PermisoDenegadoException("El rol de proceso pertenece a otra empresa");
        }
    }

    private RolProceso obtenerActivo(Long id) {
        return rolProcesoRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Rol de proceso no encontrado: " + id));
    }
}
