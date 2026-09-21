package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.empresa.CrearEmpresaRequest;
import co.javeriana.dw.proyecto.dto.empresa.EmpresaResponse;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.service.EmpresaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de HU-01 a nivel HTTP: validacion de entrada y codigos de respuesta.
 * EmpresaService esta mockeado — aqui no interesa la logica de negocio (ya probada
 * en EmpresaServiceTest), solo que el controller conecte bien el request/response
 * y que Spring traduzca las excepciones a los codigos correctos.
 */
@WebMvcTest(EmpresaController.class)
@Import(ManejadorGlobalExcepciones.class)
class EmpresaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmpresaService empresaService;

    private CrearEmpresaRequest requestValido() {
        CrearEmpresaRequest request = new CrearEmpresaRequest();
        request.setNombre("Acme S.A.S.");
        request.setRuc("900123456-7");
        request.setRazonSocial("Acme Sociedad Anonima");
        request.setEmail("contacto@acme.com");
        request.setAdminNombre("Juan Perez");
        request.setAdminPassword("claveSegura123");
        return request;
    }

    @Test
    void crear_devuelve201_cuandoRequestValido() throws Exception {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(1L);
        response.setNombre("Acme S.A.S.");
        response.setAdminUsuarioId(99L);
        when(empresaService.crear(any())).thenReturn(response);

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.adminUsuarioId").value(99));
    }

    @Test
    void crear_devuelve400_cuandoFaltaNombre() throws Exception {
        CrearEmpresaRequest request = requestValido();
        request.setNombre("");

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_devuelve400_cuandoEmailInvalido() throws Exception {
        CrearEmpresaRequest request = requestValido();
        request.setEmail("no-es-un-correo");

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_devuelve400_cuandoPasswordMuyCorta() throws Exception {
        CrearEmpresaRequest request = requestValido();
        request.setAdminPassword("123");

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_devuelve409_cuandoRucDuplicado() throws Exception {
        when(empresaService.crear(any()))
                .thenThrow(new NombreDuplicadoException("Ya existe una empresa con ese RUC"));

        mockMvc.perform(post("/api/empresas")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    void obtener_devuelve200_cuandoExiste() throws Exception {
        EmpresaResponse response = new EmpresaResponse();
        response.setId(1L);
        response.setNombre("Acme S.A.S.");
        when(empresaService.obtenerPorId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/empresas/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Acme S.A.S."));
    }

    @Test
    void obtener_devuelve404_cuandoNoExiste() throws Exception {
        when(empresaService.obtenerPorId(404L))
                .thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/empresas/404"))
                .andExpect(status().isNotFound());
    }
}