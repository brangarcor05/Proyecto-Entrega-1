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
import org.junit.jupiter.api.BeforeEach;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.context.support.WithMockUser;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
@WithMockUser(roles = "ADMIN") 
class EmpresaControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;
    @BeforeEach
    void setup() {
            mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())   
            .build();
                    }

    @MockBean
    private EmpresaService empresaService;

    private CrearEmpresaRequest requestValido() {
        CrearEmpresaRequest request = new CrearEmpresaRequest();
        request.setNombre("Acme S.A.S.");
        request.setRut("900123456-7");
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
                        .with(csrf())
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
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_devuelve400_cuandoEmailInvalido() throws Exception {
        CrearEmpresaRequest request = requestValido();
        request.setEmail("no-es-un-correo");

        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_devuelve400_cuandoPasswordMuyCorta() throws Exception {
        CrearEmpresaRequest request = requestValido();
        request.setAdminPassword("123");

        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
                        .contentType(   "application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crear_devuelve409_cuandoRutDuplicado() throws Exception {
        when(empresaService.crear(any()))
                .thenThrow(new NombreDuplicadoException("Ya existe una empresa con ese RUT"));

        mockMvc.perform(post("/api/empresas")
                        .with(csrf())
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

        mockMvc.perform(get("/api/empresas/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Acme S.A.S."));
    }

    @Test
    void obtener_devuelve404_cuandoNoExiste() throws Exception {
        when(empresaService.obtenerPorId(404L))
                .thenThrow(new RecursoNoEncontradoException("no existe"));

        mockMvc.perform(get("/api/empresas/404")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}