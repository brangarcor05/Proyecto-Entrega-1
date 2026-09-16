package co.javeriana.dw.proyecto.dto.arco;

import java.util.List;

/**
 * Resultado de eliminar un arco (HU-13).
 *
 * La historia pide que el editor advierta si la eliminacion deja un elemento sin
 * camino de entrada o de salida, pero no que la impida: un diagrama en borrador
 * puede estar incompleto. Por eso la respuesta lleva cuerpo con las advertencias
 * en vez del 204 habitual de un borrado.
 */
public record ArcoEliminadoResponse(
        Long id,
        List<String> advertencias) {
}
