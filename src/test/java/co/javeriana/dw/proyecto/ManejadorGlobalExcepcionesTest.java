package co.javeriana.dw.proyecto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import co.javeriana.dw.proyecto.exception.ErrorResponse;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.exception.PermisoDenegadoException;
import co.javeriana.dw.proyecto.exception.ReglaNegocioException;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ManejadorGlobalExcepcionesTest {

    private final ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();

    /**
     * Spring 6 / Boot 3 marca el primer argumento de
     * {@link MethodArgumentNotValidException} como {@code @NonNull}.
     * Como en el test solo nos interesa el {@code BindingResult},
     * construimos un MethodParameter "dummy" reutilizable.
     */
    private static MethodParameter methodParameterDummy() {
        try {
            Method metodo = String.class.getDeclaredMethod("length");
            return new MethodParameter(metodo, -1);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No se pudo crear MethodParameter dummy", e);
        }
    }

    // ------------------------------------------------------------------
    // manejarReglaNegocio → 409 CONFLICT
    // ------------------------------------------------------------------
    @Test
    @DisplayName("manejarReglaNegocio debe responder 409 CONFLICT con el mensaje")
    void manejarReglaNegocioDevuelve409() {
        ReglaNegocioException ex = new ReglaNegocioException("La empresa ya tiene admin");

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarReglaNegocio(ex);

        assertEquals(HttpStatus.CONFLICT, respuesta.getStatusCode());
        ErrorResponse body = respuesta.getBody();
        assertNotNull(body);
        assertEquals(409, body.estado());
        assertEquals("Conflicto", body.error());
        assertEquals("La empresa ya tiene admin", body.mensaje());
        assertNull(body.camposInvalidos());
        assertNotNull(body.momento());
    }

    // ------------------------------------------------------------------
    // manejarPermisoDenegado → 403 FORBIDDEN
    // ------------------------------------------------------------------
    @Test
    @DisplayName("manejarPermisoDenegado debe responder 403 FORBIDDEN con el mensaje")
    void manejarPermisoDenegadoDevuelve403() {
        PermisoDenegadoException ex = new PermisoDenegadoException("No tiene permisos");

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarPermisoDenegado(ex);

        assertEquals(HttpStatus.FORBIDDEN, respuesta.getStatusCode());
        ErrorResponse body = respuesta.getBody();
        assertNotNull(body);
        assertEquals(403, body.estado());
        assertEquals("Permiso denegado", body.error());
        assertEquals("No tiene permisos", body.mensaje());
        assertNull(body.camposInvalidos());
    }

    // ------------------------------------------------------------------
    // manejarArgumentoInvalido → 400 BAD_REQUEST
    // ------------------------------------------------------------------
    @Test
    @DisplayName("manejarArgumentoInvalido debe responder 400 BAD_REQUEST con el mensaje")
    void manejarArgumentoInvalidoDevuelve400() {
        IllegalArgumentException ex = new IllegalArgumentException("El parámetro X es inválido");

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarArgumentoInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        ErrorResponse body = respuesta.getBody();
        assertNotNull(body);
        assertEquals(400, body.estado());
        assertEquals("Peticion invalida", body.error());
        assertEquals("El parámetro X es inválido", body.mensaje());
        assertNull(body.camposInvalidos());
    }

    // ------------------------------------------------------------------
    // manejarValidacion → 400 con camposInvalidos
    // ------------------------------------------------------------------
    @Test
    @DisplayName("manejarValidacion debe responder 400 con el mapa de campos inválidos")
    void manejarValidacionDevuelve400ConCampos() {
        // Construimos un MethodArgumentNotValidException real
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "empresaForm");
        bindingResult.addError(new FieldError(
                "empresaForm", "nombre", "no puede ser nulo"));
        bindingResult.addError(new FieldError(
                "empresaForm", "email", "formato inválido"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameterDummy(), bindingResult);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarValidacion(ex);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
        ErrorResponse body = respuesta.getBody();
        assertNotNull(body);
        assertEquals(400, body.estado());
        assertEquals("Validacion fallida", body.error());

        Map<String, String> campos = body.camposInvalidos();
        assertNotNull(campos);
        assertEquals(2, campos.size());
        assertEquals("no puede ser nulo", campos.get("nombre"));
        assertEquals("formato inválido", campos.get("email"));
    }

    @Test
    @DisplayName("manejarValidacion conserva el primer mensaje si hay campos repetidos")
    void manejarValidacionConservaPrimerMensajePorCampo() {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "empresaForm");
        bindingResult.addError(new FieldError(
                "empresaForm", "nombre", "primer mensaje"));
        bindingResult.addError(new FieldError(
                "empresaForm", "nombre", "segundo mensaje"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(methodParameterDummy(), bindingResult);

        ResponseEntity<ErrorResponse> respuesta = manejador.manejarValidacion(ex);

        Map<String, String> campos = respuesta.getBody().camposInvalidos();
        assertEquals(1, campos.size());
        assertEquals("primer mensaje", campos.get("nombre"));
    }
}