package co.javeriana.dw.proyecto;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ThymeleafApplicationTest {

    @Test
    void mainDelegaEnSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = Mockito.mockStatic(SpringApplication.class)) {
            ThymeleafApplication.main(new String[]{});

            springApp.verify(() ->
                    SpringApplication.run(eq(ThymeleafApplication.class), any(String[].class)));
        }
    }
}