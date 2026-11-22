package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.gateway.ActualizarGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.GatewayResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Gateway;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;
import co.javeriana.dw.proyecto.repository.GatewayRepository;

/**
 * Gateways del diagrama (HU-14 a HU-16).
 *
 * Mismas reglas de acceso que el resto del modelado: crear y editar, administrador o
 * editor; eliminar, solo administrador; y solo la empresa propietaria modifica el
 * proceso. Cada cambio queda en el historial.
 */
@Service
public class GatewayService {

    private static final String ENTIDAD = "Gateway";

    private final GatewayRepository gatewayRepository;
    private final PermisoService permisoService;
    private final AccesoProcesoService accesoProcesoService;
    private final HistorialService historialService;
    private final ModelMapper modelMapper;

    public GatewayService(GatewayRepository gatewayRepository, PermisoService permisoService,
            AccesoProcesoService accesoProcesoService, HistorialService historialService,
            ModelMapper modelMapper) {
        this.gatewayRepository = gatewayRepository;
        this.permisoService = permisoService;
        this.accesoProcesoService = accesoProcesoService;
        this.historialService = historialService;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public GatewayResponse crear(CrearGatewayRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        Proceso proceso = accesoProcesoService.obtenerProcesoActivo(request.getProcesoId());
        accesoProcesoService.validarEscritura(usuario, proceso);
        Pool pool = obtenerPoolConElementos(request.getPoolId(), proceso);

        Gateway gateway = modelMapper.map(request, Gateway.class);
        gateway.setId(null);
        gateway.setProceso(proceso);
        gateway.setPool(pool);
        gateway.setCanvasId(request.getCanvasId());
        gateway.setActivo(true);
        // El nombre es opcional en el diagrama, pero la columna no admite nulos.
        gateway.setNombre(nombreONombrePorDefecto(request.getNombre(), gateway));

        if (gateway.getPosicionX() == null)
            gateway.setPosicionX(0.0);
        if (gateway.getPosicionY() == null)
            gateway.setPosicionY(0.0);

        gateway = gatewayRepository.save(gateway);

        historialService.registrar(ENTIDAD, gateway.getId(), AccionHistorial.CREACION,
                usuario, proceso, "Gateway " + gateway.getTipo() + " creado: " + gateway.getNombre());
        return mapearAResponse(gateway);
    }

    @Transactional(readOnly = true)
    public GatewayResponse obtenerPorId(Long id, Long empresaId) {
        Gateway gateway = obtenerActivo(id);
        accesoProcesoService.validarLectura(gateway.getProceso().getId(), empresaId);
        return mapearAResponse(gateway);
    }

    @Transactional(readOnly = true)
    public List<GatewayResponse> listarPorProceso(Long procesoId, Long empresaId) {
        accesoProcesoService.validarLectura(procesoId, empresaId);
        return gatewayRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .toList();
    }

    @Transactional
    public GatewayResponse actualizar(Long id, ActualizarGatewayRequest request, Long usuarioId) {
        Usuario usuario = permisoService.validarPuedeEditar(usuarioId);
        Gateway gateway = obtenerActivo(id);
        Proceso proceso = gateway.getProceso();
        accesoProcesoService.validarEscritura(usuario, proceso);

        if (request.getPoolId() != null) {
            gateway.setPool(obtenerPoolConElementos(request.getPoolId(), proceso));
        }

        gateway.setTipo(request.getTipo());
        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            gateway.setNombre(request.getNombre().trim());
        }
        if (request.getCanvasId() != null)
            gateway.setCanvasId(request.getCanvasId());
        if (request.getPosicionX() != null)
            gateway.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null)
            gateway.setPosicionY(request.getPosicionY());

        gateway = gatewayRepository.save(gateway);

        historialService.registrar(ENTIDAD, gateway.getId(), AccionHistorial.EDICION,
                usuario, proceso, "Gateway actualizado: " + gateway.getNombre() + " (" + gateway.getTipo() + ")");
        return mapearAResponse(gateway);
    }

    /** HU-16: eliminacion logica, solo el administrador. */
    @Transactional
    public void eliminar(Long id, Long usuarioId) {
        Usuario usuario = permisoService.validarEsAdministrador(usuarioId);
        Gateway gateway = obtenerActivo(id);
        accesoProcesoService.validarEscritura(usuario, gateway.getProceso());

        gateway.setActivo(false);
        gatewayRepository.save(gateway);

        historialService.registrar(ENTIDAD, gateway.getId(), AccionHistorial.ELIMINACION,
                usuario, gateway.getProceso(), "Gateway marcado como inactivo: " + gateway.getNombre());
    }

    private Gateway obtenerActivo(Long id) {
        return gatewayRepository.findById(id)
                .filter(Gateway::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));
    }

    private Pool obtenerPoolConElementos(Long poolId, Proceso proceso) {
        Pool pool = accesoProcesoService.obtenerPoolDelProceso(poolId, proceso);
        if (pool.isCajaNegra()) {
            throw new ReglaNegocioException("Un pool caja negra no puede contener gateways");
        }
        return pool;
    }

    private String nombreONombrePorDefecto(String nombre, Gateway gateway) {
        if (nombre != null && !nombre.isBlank()) {
            return nombre.trim();
        }
        return "Gateway " + (gateway.getTipo() != null ? gateway.getTipo().name().toLowerCase() : "");
    }

    private GatewayResponse mapearAResponse(Gateway gateway) {
        GatewayResponse response = modelMapper.map(gateway, GatewayResponse.class);
        if (gateway.getProceso() != null)
            response.setProcesoId(gateway.getProceso().getId());
        if (gateway.getPool() != null)
            response.setPoolId(gateway.getPool().getId());
        return response;
    }
}
