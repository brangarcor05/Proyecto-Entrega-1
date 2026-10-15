package co.javeriana.dw.proyecto.service;

import java.util.List;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import co.javeriana.dw.proyecto.dto.gateway.ActualizarGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.GatewayResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.EstadoProceso;
import co.javeriana.dw.proyecto.entidad.Gateway;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.GatewayRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;

@Service
public class GatewayService {
    private final GatewayRepository gatewayRepository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final PermisoService permisoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public GatewayService(GatewayRepository gatewayRepository, ProcesoRepository procesoRepository,
            PoolRepository poolRepository, PermisoService permisoService,
            HistorialService historialService, ModelMapper modelMapper) {
        this.gatewayRepository = gatewayRepository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.permisoService = permisoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public GatewayResponse crear(CrearGatewayRequest request, Long usuarioId) {
        Proceso proceso = obtenerProcesoActivo(request.getProcesoId());
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, proceso);
        Pool pool = obtenerPoolDelProceso(request.getPoolId(), proceso);
        Gateway gateway = new Gateway();
        gateway.setNombre(request.getNombre());
        gateway.setTipo(request.getTipo());
        gateway.setPosicionX(request.getPosicionX());
        gateway.setPosicionY(request.getPosicionY());
        gateway.setProceso(proceso);
        gateway.setPool(pool);
        gateway.setActivo(true);
        gateway = gatewayRepository.save(gateway);
        historialService.registrar("Gateway", gateway.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Gateway creado: " + gateway.getNombre());
        return mapear(gateway);
    }

    @Transactional(readOnly = true)
    public GatewayResponse obtenerPorId(Long id) { return mapear(obtenerActivo(id)); }

    @Transactional(readOnly = true)
    public List<GatewayResponse> listarPorProceso(Long procesoId) {
        obtenerProcesoActivo(procesoId);
        return gatewayRepository.findByProcesoIdAndActivoTrue(procesoId).stream().map(this::mapear).toList();
    }

    @Transactional
    public GatewayResponse actualizar(Long id, ActualizarGatewayRequest request, Long usuarioId) {
        Gateway gateway = obtenerActivo(id);
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        validarEsDeLaEmpresa(usuario, gateway.getProceso());
        gateway.setNombre(request.getNombre());
        gateway.setTipo(request.getTipo());
        gateway.setPosicionX(request.getPosicionX());
        gateway.setPosicionY(request.getPosicionY());
        gateway = gatewayRepository.save(gateway);
        historialService.registrar("Gateway", gateway.getId(), AccionHistorial.EDICION,
                usuario, gateway.getProceso(), "Gateway actualizado: " + gateway.getNombre());
        return mapear(gateway);
    }

    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Gateway gateway = obtenerActivo(id);
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        validarEsDeLaEmpresa(usuario, gateway.getProceso());
        gateway.setActivo(false);
        gatewayRepository.save(gateway);
        historialService.registrar("Gateway", gateway.getId(), AccionHistorial.ELIMINACION,
                usuario, gateway.getProceso(), "Gateway marcado como inactivo: " + gateway.getNombre());
    }

    private Pool obtenerPoolDelProceso(Long id, Proceso proceso) {
        Pool pool = poolRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + id));
        if (!pool.getProceso().getId().equals(proceso.getId())) {
            throw new ReglaNegocioException("El pool pertenece a otro proceso");
        }
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener gateways");
        }
        return pool;
    }

    private void validarEsDeLaEmpresa(Usuario usuario, Proceso proceso) {
        if (!usuario.getEmpresa().getId().equals(proceso.getEmpresa().getId())) {
            throw new PermisoDenegadoException("El proceso pertenece a otra empresa");
        }
    }

    private Gateway obtenerActivo(Long id) {
        return gatewayRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));
    }

    private Proceso obtenerProcesoActivo(Long id) {
        return procesoRepository.findByIdAndEstadoNot(id, EstadoProceso.INACTIVO)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + id));
    }

    private GatewayResponse mapear(Gateway gateway) {
        GatewayResponse response = modelMapper.map(gateway, GatewayResponse.class);
        response.setProcesoId(gateway.getProceso().getId());
        response.setPoolId(gateway.getPool().getId());
        return response;
    }
}
