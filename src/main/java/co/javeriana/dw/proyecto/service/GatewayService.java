package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.gateway.ActualizarGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.GatewayResponse;
import co.javeriana.dw.proyecto.entidad.Gateway;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.GatewayRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GatewayService {

    private final GatewayRepository gatewayRepository;
    private final ProcesoRepository procesoRepository;
    private final PoolRepository poolRepository;
    private final ModelMapper modelMapper;

    public GatewayService(GatewayRepository gatewayRepository, ProcesoRepository procesoRepository,
            PoolRepository poolRepository, ModelMapper modelMapper) {
        this.gatewayRepository = gatewayRepository;
        this.procesoRepository = procesoRepository;
        this.poolRepository = poolRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public GatewayResponse crear(CrearGatewayRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(
                        () -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        Pool pool = poolRepository.findById(request.getPoolId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + request.getPoolId()));

        Gateway gateway = modelMapper.map(request, Gateway.class);
        gateway.setProceso(proceso);
        gateway.setPool(pool);
        gateway.setCanvasId(request.getCanvasId());
        gateway.setActivo(true);

        if (gateway.getPosicionX() == null)
            gateway.setPosicionX(0.0);
        if (gateway.getPosicionY() == null)
            gateway.setPosicionY(0.0);

        gateway = gatewayRepository.save(gateway);
        return mapearAResponse(gateway);
    }

    public GatewayResponse obtenerPorId(Long id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));
        return mapearAResponse(gateway);
    }

    public List<GatewayResponse> listarPorProceso(Long procesoId) {
        return gatewayRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public GatewayResponse actualizar(Long id, ActualizarGatewayRequest request) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));

        if (request.getPoolId() != null) {
            Pool pool = poolRepository.findById(request.getPoolId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + request.getPoolId()));
            gateway.setPool(pool);
        }

        gateway.setNombre(request.getNombre());
        gateway.setTipo(request.getTipo());
        gateway.setCanvasId(request.getCanvasId());

        if (request.getPosicionX() != null)
            gateway.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null)
            gateway.setPosicionY(request.getPosicionY());

        gateway = gatewayRepository.save(gateway);
        return mapearAResponse(gateway);
    }

    @Transactional
    public void eliminar(Long id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));
        gateway.setActivo(false);
        gatewayRepository.save(gateway);
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