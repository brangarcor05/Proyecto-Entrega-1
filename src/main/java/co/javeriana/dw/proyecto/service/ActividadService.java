package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.actividad.ActualizarActividadRequest;
import co.javeriana.dw.proyecto.dto.actividad.ActividadResponse;
import co.javeriana.dw.proyecto.dto.actividad.CrearActividadRequest;
import co.javeriana.dw.proyecto.entidad.Actividad;
import co.javeriana.dw.proyecto.entidad.Lane;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.repository.ActividadRepository;
import co.javeriana.dw.proyecto.repository.LaneRepository;
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
    private final ModelMapper modelMapper;

    public ActividadService(ActividadRepository actividadRepository, ProcesoRepository procesoRepository, LaneRepository laneRepository, ModelMapper modelMapper) {
        this.actividadRepository = actividadRepository;
        this.procesoRepository = procesoRepository;
        this.laneRepository = laneRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public ActividadResponse crear(CrearActividadRequest request) {
        Proceso proceso = procesoRepository.findById(request.getProcesoId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Proceso no encontrado: " + request.getProcesoId()));

        Lane lane = laneRepository.findById(request.getLaneId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + request.getLaneId()));

        Actividad actividad = modelMapper.map(request, Actividad.class);
        actividad.setProceso(proceso);
        actividad.setLane(lane);
        actividad.setActivo(true);

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

        Lane lane = laneRepository.findById(request.getLaneId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Lane no encontrada: " + request.getLaneId()));

        actividad.setNombre(request.getNombre());
        actividad.setTipo(request.getTipo());
        actividad.setLane(lane);

        if (request.getPosicionX() != null) actividad.setPosicionX(request.getPosicionX());
        if (request.getPosicionY() != null) actividad.setPosicionY(request.getPosicionY());

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
        if (actividad.getProceso() != null) {
            response.setProcesoId(actividad.getProceso().getId());
        }
        if (actividad.getLane() != null) {
            response.setLaneId(actividad.getLane().getId());
        }
        return response;
    }
}