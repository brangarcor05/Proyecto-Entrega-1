package co.javeriana.dw.proyecto.controller;

import co.javeriana.dw.proyecto.dto.usuario.CambiarRolRequest;
import co.javeriana.dw.proyecto.dto.usuario.CrearUsuarioRequest;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.exception.ManejadorGlobalExcepciones;
import co.javeriana.dw.proyecto.exception.NombreDuplicadoException;
import co.javeriana.dw.proyecto.exception.RecursoNoEncontradoException;
import co.javeriana.dw.proyecto.service.UsuarioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de HU-02 a nivel HTTP: invitar, cambiar rol, desactivar y listar usuarios.
 * UsuarioService esta mockeado.
 */
@WebMvcTest(UsuarioController.class)
@Import(ManejadorGlobalExcepciones.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsuarioService usuarioService;

    private CrearUsuarioRequest requestValido() {
        CrearUsuarioRequest request = new CrearUsuarioRequest();
        request.setNombre("Ana Ruiz");
        request.setEmail("ana@acme.com");
        request.setRolUsuario(RolUsuario.EDITOR);
        return request;
    }

    @Test
    void invitar_devuelve201_cuandoRequestValido() throws Exception {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(20L);
        response.setNombre("Ana Ruiz");
        response.setRolUsuario(RolUsuario.EDITOR);
        when(usuarioService.invitarUsuario(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/usuarios")
                        .param("empresaId", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.rolUsuario").value("EDITOR"));
    }

    @Test
    void invitar_devuelve400_cuandoFaltaEmpresaId() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitar_devuelve400_cuandoEmailInvalido() throws Exception {
        CrearUsuarioRequest request = requestValido();
        request.setEmail("no-es-correo");

        mockMvc.perform(post("/api/usuarios")
                        .param("empresaId", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitar_devuelve400_cuandoFaltaRolAcceso() throws Exception {
        CrearUsuarioRequest request = requestValido();
        request.setRolUsuario(null);

        mockMvc.perform(post("/api/usuarios")
                        .param("empresaId", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitar_devuelve409_cuandoCorreoDuplicado() throws Exception {
        when(usuarioService.invitarUsuario(any(), any()))
                .thenThrow(new NombreDuplicadoException("El correo ya esta registrado"));

        mockMvc.perform(post("/api/usuarios")
                        .param("empresaId", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestValido())))
                .andExpect(status().isConflict());
    }

    @Test
    void cambiarRol_devuelve200_cuandoValido() throws Exception {
        UsuarioResponse response = new UsuarioResponse();
        response.setId(5L);
        response.setRolUsuario(RolUsuario.ADMIN);
        when(usuarioService.cambiarRolAcceso(eq(5L), any())).thenReturn(response);

        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        mockMvc.perform(patch("/api/usuarios/5/rol")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rolUsuario").value("ADMIN"));
    }

    @Test
    void cambiarRol_devuelve404_cuandoUsuarioNoExiste() throws Exception {
        when(usuarioService.cambiarRolAcceso(eq(99L), any()))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado"));

        CambiarRolRequest request = new CambiarRolRequest();
        request.setRolUsuario(RolUsuario.ADMIN);

        mockMvc.perform(patch("/api/usuarios/99/rol")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void cambiarRol_devuelve400_cuandoFaltaRol() throws Exception {
        CambiarRolRequest request = new CambiarRolRequest();

        mockMvc.perform(patch("/api/usuarios/5/rol")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desactivar_devuelve204() throws Exception {
        mockMvc.perform(delete("/api/usuarios/7"))
                .andExpect(status().isNoContent());
    }

    @Test
    void listarPorEmpresa_devuelve200ConLista() throws Exception {
        UsuarioResponse u1 = new UsuarioResponse();
        u1.setId(1L);
        when(usuarioService.listarPorEmpresa(1L)).thenReturn(List.of(u1));

        mockMvc.perform(get("/api/usuarios").param("empresaId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}