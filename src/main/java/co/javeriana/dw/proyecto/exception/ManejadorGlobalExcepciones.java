package co.javeriana.dw.proyecto.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Manejo centralizado de errores de toda la API. Es infraestructura compartida:
 * los handlers de empresas, usuarios y procesos van todos aqui. Declarar un segundo
 * @RestControllerAdvice que atienda la misma excepcion impide que la aplicacion arranque.
 */
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.de(404, "No encontrado", ex.getMessage()));
    }

    @ExceptionHandler(NombreDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarDuplicado(NombreDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.de(409, "Conflicto", ex.getMessage()));
    }

    @ExceptionHandler(PermisoDenegadoException.class)
    public ResponseEntity<ErrorResponse> manejarPermisoDenegado(PermisoDenegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.de(403, "Permiso denegado", ex.getMessage()));
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> manejarCredenciales(CredencialesInvalidasException ex) {
        // HU-03: mensaje generico a proposito, sin revelar si el correo existe.
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.de(401, "No autorizado", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarArgumentoInvalido(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.de(400, "Peticion invalida", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.deValidacion(400, "Validacion fallida",
                        "Hay campos obligatorios sin diligenciar o con un valor invalido", campos));
    }
}
