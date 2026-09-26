package co.javeriana.dw.proyecto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que la aplicacion arranque. Spring Data valida los metodos derivados de
 * los repositorios al levantar el contexto, no al compilar: sin esta prueba, un
 * metodo cuyo nombre no coincida con un campo de la entidad compila bien y tumba
 * la aplicacion en ejecucion. Ya nos paso cuatro veces.
 */
@SpringBootTest
class ContextoCargaTest {

    @Test
    void elContextoDeSpringLevanta() {
    }
}
