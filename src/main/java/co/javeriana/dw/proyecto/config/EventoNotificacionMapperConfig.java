package co.javeriana.dw.proyecto.config;

import co.javeriana.dw.proyecto.dto.comun.CampoDatoDto;
import co.javeriana.dw.proyecto.dto.evento.EventoResponse;
import co.javeriana.dw.proyecto.dto.notificacionexterna.NotificacionExternaResponse;
import co.javeriana.dw.proyecto.entidad.CampoDato;
import co.javeriana.dw.proyecto.entidad.Evento;
import co.javeriana.dw.proyecto.entidad.EventoNotificacionExterna;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

/** Mapeos de eventos y notificaciones. Los DTO record se construyen con convertidores. */
@Configuration
public class EventoNotificacionMapperConfig {

    public EventoNotificacionMapperConfig(ModelMapper mapper) {
        mapper.createTypeMap(CampoDato.class, CampoDatoDto.class).setConverter(context -> {
            CampoDato campo = context.getSource();
            return new CampoDatoDto(campo.getNombre(), campo.getTipoDato());
        });
        mapper.createTypeMap(CampoDatoDto.class, CampoDato.class).setConverter(context -> {
            CampoDatoDto campo = context.getSource();
            return new CampoDato(campo.nombre(), campo.tipoDato());
        });
        mapper.createTypeMap(Evento.class, EventoResponse.class).setConverter(context -> {
            Evento evento = context.getSource();
            return new EventoResponse(
                    evento.getId(), evento.getProceso().getId(), evento.getPool().getId(),
                    evento.getNombre(), evento.getTipo(), evento.getPosicionX(),
                    evento.getPosicionY(), evento.isActivo());
        });
        mapper.createTypeMap(EventoNotificacionExterna.class, NotificacionExternaResponse.class)
                .setConverter(context -> {
                    EventoNotificacionExterna notificacion = context.getSource();
                    return new NotificacionExternaResponse(
                            notificacion.getId(), notificacion.getProceso().getId(),
                            notificacion.getPool().getId(), notificacion.getPoolDestino().getId(),
                            notificacion.getNombre(), notificacion.getTipoDestino(),
                            notificacion.getMomentoProceso(), notificacion.getAccionSiFalla(),
                            notificacion.getActividadManejoError() == null
                                    ? null : notificacion.getActividadManejoError().getId(),
                            notificacion.getCampos().stream()
                                    .map(campo -> mapper.map(campo, CampoDatoDto.class)).toList(),
                            notificacion.getPosicionX(), notificacion.getPosicionY(),
                            notificacion.isActivo());
                });
    }
}
