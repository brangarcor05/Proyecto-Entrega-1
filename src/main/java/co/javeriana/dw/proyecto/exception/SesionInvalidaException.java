package co.javeriana.dw.proyecto.exception;

/**
 * El refresh token no existe, venció, fue revocado o pertenece a un usuario inactivo.
 * El mensaje es siempre el mismo para no dar pistas de cuál de los casos fue.
 */
public class SesionInvalidaException extends RuntimeException {
    public SesionInvalidaException() {
        super("La sesión expiró o no es válida. Inicie sesión de nuevo.");
    }
}
