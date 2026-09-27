package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.gateway.ActualizarGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.CrearGatewayRequest;
import co.javeriana.dw.proyecto.dto.gateway.GatewayResponse;
import co.javeriana.dw.proyecto.entidad.Gateway;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.GatewayRepository;
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
    private final ModelMapper modelMapper;

    public GatewayService(GatewayRepository gatewayRepository, ProcesoRepository procesoRepository, ModelMapper modelMapper) {
        this.gatewayRepository = gatewayRepository;
        this.procesoRepository = procesoRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public GatewayResponse crear(CrearGatewayRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        Gateway gateway = modelMapper.map(request, Gateway.class);
        gateway.setProceso(proceso);
        gateway.setActivo(true);

        gateway = gatewayRepository.save(gateway);
        return modelMapper.map(gateway, GatewayResponse.class);
    }

    public GatewayResponse obtenerPorId(Long id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));
        return modelMapper.map(gateway, GatewayResponse.class);
    }

    public List<GatewayResponse> listarPorProceso(Long procesoId) {
        return gatewayRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(gateway -> modelMapper.map(gateway, GatewayResponse.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public GatewayResponse actualizar(Long id, ActualizarGatewayRequest request) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));

        gateway.setNombre(request.getNombre());
        gateway.setTipo(request.getTipo());
        if (request.getPosicionX() != null) gateway.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null) gateway.setPosicionY(request.getPosicionY());

        gateway = gatewayRepository.save(gateway);
        return modelMapper.map(gateway, GatewayResponse.class);
    }

    @Transactional
    public void eliminar(Long id) {
        Gateway gateway = gatewayRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Gateway no encontrado: " + id));
        gateway.setActivo(false);
        gatewayRepository.save(gateway);
    }
}