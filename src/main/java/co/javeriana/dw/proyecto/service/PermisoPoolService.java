package co.javeriana.dw.proyecto.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.permisopool.ConfigurarPermisoPoolRequest;
import co.javeriana.dw.proyecto.dto.permisopool.PermisoPoolResponse;
import co.javeriana.dw.proyecto.entidad.PermisoPool;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.EmpresaRepository;
import co.javeriana.dw.proyecto.repository.PermisoPoolRepository;

/**
 * HU-24: cada empresa define que roles de acceso pueden crear, editar o eliminar
 * pools y lanes. Mientras no configure nada rigen los valores por defecto, que son
 * los mismos del resto del sistema.
 */
@Service
public class PermisoPoolService {

    private final PermisoPoolRepository permisoPoolRepository;
    private final EmpresaRepository empresaRepository;
    private final PermisoService permisoService;

    public PermisoPoolService(PermisoPoolRepository permisoPoolRepository,
                              EmpresaRepository empresaRepository, PermisoService permisoService) {
        this.permisoPoolRepository = permisoPoolRepository;
        this.empresaRepository = empresaRepository;
        this.permisoService = permisoService;
    }

    /**
     * Devuelve los tres roles de acceso con lo que puede hacer cada uno. Los que la
     * empresa no ha tocado llegan con {@code configurado} en false y los valores
     * por defecto, para que se vea que rige aunque nadie lo haya definido.
     */
    @Transactional(readOnly = true)
    public List<PermisoPoolResponse> consultar(Long empresaId) {
        if (!empresaRepository.existsById(empresaId)) {
            throw new RecursoNoEncontradoException("No existe la empresa " + empresaId);
        }

        List<PermisoPoolResponse> permisos = new ArrayList<>();
        for (RolUsuario rol : RolUsuario.values()) {
            permisos.add(permisoPoolRepository.findByEmpresaIdAndRolUsuario(empresaId, rol)
                    .map(PermisoPoolResponse::desde)
                    .orElseGet(() -> PermisoPoolResponse.porDefecto(empresaId, rol)));
        }
        return permisos;
    }

    /**
     * Configura un rol. Solo el administrador, y solo sobre su propia empresa: la
     * configuracion se toma del usuario autenticado, no de un parametro, para que
     * nadie pueda cambiar los permisos de otra organizacion.
     */
    @Transactional
    public PermisoPoolResponse configurar(ConfigurarPermisoPoolRequest request, Long usuarioId) {
        Usuario administrador = permisoService.validarEsAdministrador(usuarioId);
        Long empresaId = administrador.getEmpresa().getId();

        PermisoPool permiso = permisoPoolRepository
                .findByEmpresaIdAndRolUsuario(empresaId, request.rolUsuario())
                .orElseGet(() -> {
                    PermisoPool nuevo = new PermisoPool();
                    nuevo.setEmpresa(administrador.getEmpresa());
                    nuevo.setRolUsuario(request.rolUsuario());
                    return nuevo;
                });

        permiso.setPuedeCrear(request.puedeCrear());
        permiso.setPuedeEditar(request.puedeEditar());
        permiso.setPuedeEliminar(request.puedeEliminar());

        return PermisoPoolResponse.desde(permisoPoolRepository.save(permiso));
    }
}
