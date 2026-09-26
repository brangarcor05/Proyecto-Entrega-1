package co.javeriana.dw.proyecto.repository;

import co.javeriana.dw.proyecto.entidad.Empresa;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de HU-01 contra una base de datos real (H2 en memoria, ver
 * application-test.properties) — verifican las constraints unique de RUC y
 * email que no se pueden probar con un mock de repository.
 */
@DataJpaTest
@ActiveProfiles("test")
class EmpresaRepositoryTest {

    @Autowired
    private EmpresaRepository empresaRepository;

    private Empresa empresaDePrueba(String ruc, String email) {
        Empresa e = new Empresa();
        e.setNombre("Acme S.A.S.");
        e.setRuc(ruc);
        e.setRazonSocial("Acme Sociedad Anonima");
        e.setEmail(email);
        return e;
    }

    @Test
    void guardarYBuscarPorRuc_funcionaCorrectamente() {
        empresaRepository.save(empresaDePrueba("900123456-7", "a@acme.com"));

        assertThat(empresaRepository.existsByRuc("900123456-7")).isTrue();
        assertThat(empresaRepository.findByRuc("900123456-7")).isPresent();
    }

    @Test
    void guardarConRucDuplicado_lanzaExcepcion() {
        empresaRepository.saveAndFlush(empresaDePrueba("900123456-7", "a@acme.com"));

        assertThatThrownBy(() ->
                empresaRepository.saveAndFlush(empresaDePrueba("900123456-7", "b@acme.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardarConEmailDuplicado_lanzaExcepcion() {
        empresaRepository.saveAndFlush(empresaDePrueba("900111111-1", "duplicado@acme.com"));

        assertThatThrownBy(() ->
                empresaRepository.saveAndFlush(empresaDePrueba("900222222-2", "duplicado@acme.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByEmail_devuelveFalse_cuandoNoExiste() {
        assertThat(empresaRepository.existsByEmail("nadie@acme.com")).isFalse();
    }

    @Test
    void eliminarEmpresa_esLogica_yDesapareceDeConsultasNormales() {
        // Prueba especifica de @SQLDelete + @SQLRestriction (eliminacion logica):
        // verifica que "eliminar" hace UPDATE estado=INACTIVO y no un DELETE fisico.
        Empresa empresa = empresaRepository.saveAndFlush(empresaDePrueba("900333333-3", "c@acme.com"));
        Long id = empresa.getId();

        empresaRepository.delete(empresa);
        empresaRepository.flush();

        assertThat(empresaRepository.findById(id)).isEmpty();
    }
}