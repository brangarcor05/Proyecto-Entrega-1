package co.javeriana.dw.proyecto.service;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.javeriana.dw.proyecto.dto.historial.HistorialResponse;
import co.javeriana.dw.proyecto.entidad.AccionHistorial;
import co.javeriana.dw.proyecto.entidad.Historial;
import co.javeriana.dw.proyecto.entidad.Proceso;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.repository.HistorialRepository;

@Service
public class HistorialService {

    private final HistorialRepository historialRepository;
    private final ModelMapper modelMapper;

    public HistorialService(HistorialRepository historialRepository,
                          ModelMapper modelMapper) {
        this.historialRepository = historialRepository;
        this.modelMapper = modelMapper;
    }

    public void registrar(String entidadTipo, Long entidadId, AccionHistorial accion,
                           Usuario usuario, Proceso proceso, String detalle) {
        Historial h = new Historial();
        h.setEntidadTipo(entidadTipo);
        h.setEntidadId(entidadId);
        h.setAccion(accion);
        h.setUsuario(usuario);
        h.setProceso(proceso);
        h.setDetalle(detalle);
        historialRepository.save(h);
    }

    /** HU-07: "Se puede consultar el historial de cambios del proceso", del mas reciente al mas antiguo. */
    @Transactional(readOnly = true)
    public List<HistorialResponse> listarPorProceso(Long procesoId) {
        return historialRepository.findByProcesoIdOrderByFechaDesc(procesoId).stream()
                .map(h -> modelMapper.map(h, HistorialResponse.class))
                .toList();
    }
}
