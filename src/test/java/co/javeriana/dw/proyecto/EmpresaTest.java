package co.javeriana.dw.proyecto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import co.javeriana.dw.proyecto.entidad.Empresa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmpresaTest {

    @Test
    @DisplayName("isActiva debe retornar true cuando el estado es ACTIVO")
    void isActivaRetornaTrueCuandoEstadoEsActivo() {
        Empresa empresa = Empresa.builder()
                .nombre("Acme")
                .rut("12345678-9")
                .razonSocial("Acme S.A.S")
                .email("contacto@acme.com")
                .estado("ACTIVO")
                .build();

        assertTrue(empresa.isActiva());
    }

    @Test
    @DisplayName("isActiva debe retornar false cuando el estado no es ACTIVO")
    void isActivaRetornaFalseCuandoEstadoNoEsActivo() {
        Empresa empresa = Empresa.builder()
                .nombre("Acme")
                .rut("12345678-9")
                .razonSocial("Acme S.A.S")
                .email("contacto@acme.com")
                .estado("INACTIVO")
                .build();

        assertFalse(empresa.isActiva());
    }

    @Test
    @DisplayName("isActiva debe retornar false cuando el estado es null")
    void isActivaRetornaFalseCuandoEstadoEsNull() {
        Empresa empresa = Empresa.builder()
                .estado(null)
                .build();

        assertFalse(empresa.isActiva());
    }

    @Test
    @DisplayName("El builder debe inicializar estado en ACTIVO por defecto")
    void builderInicializaEstadoActivoPorDefecto() {
        Empresa empresa = Empresa.builder().build();
        assertEquals("ACTIVO", empresa.getEstado());
        assertTrue(empresa.isActiva());
    }
}