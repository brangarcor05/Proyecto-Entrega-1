package co.javeriana.dw.proyecto.exception;

/**
 * La operacion es valida para el usuario, pero rompe una regla del modelo:
 * eliminar un rol que esta en uso, vaciar un pool que tiene elementos, dejar un
 * gateway sin salidas. No es un problema de permisos.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
