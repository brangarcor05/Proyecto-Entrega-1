package co.javeriana.dw.proyecto.exception;

// Sin mensaje detallado a propósito: HU-03 pide no revelar si el correo existe
public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException() { super("Correo o contraseña incorrectos"); }
}