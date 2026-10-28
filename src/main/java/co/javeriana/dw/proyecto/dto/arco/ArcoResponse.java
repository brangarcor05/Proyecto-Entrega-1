package co.javeriana.dw.proyecto.dto.arco;

import co.javeriana.dw.proyecto.entidad.Arco;

/**
 * Un flujo de secuencia del diagrama. Lleva el nombre de los dos extremos ademas
 * de su id, para que el cliente pueda dibujar la flecha sin pedir cada nodo aparte.
 */
public record ArcoResponse(
        Long id,
        Long procesoId,
        Long origenId,
        String origenNombre,
        Long destinoId,
        String destinoNombre,
        String etiqueta,
        String condicion,
        boolean activo) {

    /** Debe invocarse dentro de una transaccion: los nodos se cargan de forma perezosa. */
    public static ArcoResponse desde(Arco arco) {
        return new ArcoResponse(
                arco.getId(),
                arco.getProceso().getId(),
                arco.getOrigen().getId(),
                arco.getOrigen().getNombre(),
                arco.getDestino().getId(),
                arco.getDestino().getNombre(),
                arco.getEtiqueta(),
                arco.getCondicion(),
                arco.isActivo());
    }
}
