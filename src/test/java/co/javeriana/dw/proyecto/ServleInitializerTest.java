package co.javeriana.dw.proyecto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.junit.jupiter.api.Assertions.assertSame;

class ServletInitializerTest {

    @Test
    void configureRegistraThymeleafApplication() {
        ServletInitializer initializer = new ServletInitializer();
        SpringApplicationBuilder builder = new SpringApplicationBuilder();

        SpringApplicationBuilder resultado = initializer.configure(builder);

        assertSame(builder, resultado);
    }
}