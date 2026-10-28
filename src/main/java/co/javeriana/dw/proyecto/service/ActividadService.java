package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.actividad.ActualizarActividadRequest;
import co.javeriana.dw.proyecto.dto.actividad.ActividadResponse;
import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
import co.javeriana.dw.proyecto.repository.PoolRepository;
import co.javeriana.dw.proyecto.repository.ProcesoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ActividadService {
    private final ActividadRepository actividadRepository;
    private final ProcesoRepository procesoRepository;
    private final LaneRepository laneRepository;
    private final PoolRepository poolRepository;
    private final ModelMapper modelMapper;

    public ActividadService(ActividadRepository actividadRepository,
                            ProcesoRepository procesoRepository,
                            LaneRepository laneRepository,
                            PoolRepository poolRepository,
                            ModelMapper modelMapper) {
        this.actividadRepository = actividadRepository;
        this.procesoRepository = procesoRepository;
        this.laneRepository = laneRepository;
        this.poolRepository = poolRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ActividadResponse crear(CrearActividadRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        Pool pool = poolRepository.findById(request.getPoolId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + request.getPoolId()));

        Lane lane = laneRepository.findById(request.getLaneId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + request.getLaneId()));

        Actividad actividad = modelMapper.map(request, Actividad.class);
        actividad.setProceso(proceso);
        actividad.setPool(pool);
        actividad.setLane(lane);
        actividad.setActivo(true);
        
        // Aseguramos que el canvasId se guarde (si viene en el request)
        actividad.setCanvasId(request.getCanvasId());

        if (actividad.getPosicionX() == null) actividad.setPosicionX(0.0);
        if (actividad.getPosicionY() == null) actividad.setPosicionY(0.0);

        actividad = actividadRepository.save(actividad);
        return mapearAResponse(actividad);
    }

    public ActividadResponse obtenerPorId(Long id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada: " + id));
        return mapearAResponse(actividad);
    }

    public List<ActividadResponse> listarPorProceso(Long procesoId) {
        return actividadRepository.findByProcesoIdAndActivoTrue(procesoId).stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ActividadResponse actualizar(Long id, ActualizarActividadRequest request) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada: " + id));

        if (request.getPoolId() != null) {
            Pool pool = poolRepository.findById(request.getPoolId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Pool no encontrado: " + request.getPoolId()));
            actividad.setPool(pool);
        }

        if (request.getLaneId() != null) {
            Lane lane = laneRepository.findById(request.getLaneId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + request.getLaneId()));
            actividad.setLane(lane);
        }

        actividad.setNombre(request.getNombre());
        actividad.setTipo(request.getTipo());

        if (request.getPosicionX() != null) actividad.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null) actividad.setPosicionY(request.getPosicionY());
        
        if (request.getCanvasId() != null) actividad.setCanvasId(request.getCanvasId());

        actividad = actividadRepository.save(actividad);
        return mapearAResponse(actividad);
    }

    @Transactional
    public void eliminar(Long id) {
        Actividad actividad = actividadRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada: " + id));
        actividad.setActivo(false);
        actividadRepository.save(actividad);
    }

    private ActividadResponse mapearAResponse(Actividad actividad) {
        ActividadResponse response = modelMapper.map(actividad, ActividadResponse.class);
        if (actividad.getProceso() != null) response.setProcesoId(actividad.getProceso().getId());
        if (actividad.getPool() != null)    response.setPoolId(actividad.getPool().getId());
        if (actividad.getLane() != null)    response.setLaneId(actividad.getLane().getId());
        if (actividad.getCanvasId() != null) response.setCanvasId(actividad.getCanvasId());
        return response;
    }
}