package co.javeriana.dw.proyecto.repository;

import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pruebas de HU-02 contra H2. Documentan tambien la decision de diseno tomada
 * en el chat: el correo de Usuario es unico a nivel de sistema, no por empresa.
 */
@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Empresa nuevaEmpresa(String nombre, String rut, String email) {
        Empresa e = new Empresa();
        e.setNombre(nombre);
        e.setRut(rut);
        e.setRazonSocial(nombre);
        e.setEmail(email);
        return e;
    }

    private Usuario nuevoUsuario(Empresa empresa, String email) {
        Usuario u = new Usuario();
        u.setEmpresa(empresa);
        u.setNombre("Usuario de prueba");
        u.setEmail(email);
        u.setRolUsuario(RolUsuario.EDITOR);
        u.setPasswordHash("hash-de-prueba");
        return u;
    }

    @Test
    void findByEmpresaId_devuelveSoloUsuariosDeEsaEmpresa() {
        Empresa empresaA = entityManager.persist(nuevaEmpresa("A S.A.S.", "111", "a@a.com"));
        Empresa empresaB = entityManager.persist(nuevaEmpresa("B S.A.S.", "222", "b@b.com"));
        entityManager.persist(nuevoUsuario(empresaA, "u1@a.com"));
        entityManager.persist(nuevoUsuario(empresaA, "u2@a.com"));
        entityManager.persist(nuevoUsuario(empresaB, "u1@b.com"));
        entityManager.flush();

        List<Usuario> deA = usuarioRepository.findByEmpresaId(empresaA.getId());

        assertThat(deA).hasSize(2).allMatch(u -> u.getEmpresa().getId().equals(empresaA.getId()));
    }

    @Test
    void findByEmail_encuentraElUsuarioCorrecto() {
        Empresa empresa = entityManager.persist(nuevaEmpresa("A S.A.S.", "333", "c@c.com"));
        entityManager.persist(nuevoUsuario(empresa, "buscado@x.com"));
        entityManager.flush();

        assertThat(usuarioRepository.findByEmail("buscado@x.com")).isPresent();
        assertThat(usuarioRepository.findByEmail("no-existe@x.com")).isEmpty();
    }

    @Test
    void existsByEmail_esGlobal_noPorEmpresa() {
        Empresa empresaA = entityManager.persist(nuevaEmpresa("A S.A.S.", "444", "d@d.com"));
        entityManager.persist(nuevoUsuario(empresaA, "repetido@x.com"));
        entityManager.flush();

        assertThat(usuarioRepository.existsByEmail("repetido@x.com")).isTrue();
    }

    @Test
    void guardarConEmailDuplicado_enDistintasEmpresas_lanzaExcepcion() {
        Empresa empresaA = entityManager.persist(nuevaEmpresa("A S.A.S.", "555", "e@e.com"));
        Empresa empresaB = entityManager.persist(nuevaEmpresa("B S.A.S.", "666", "f@f.com"));
        usuarioRepository.saveAndFlush(nuevoUsuario(empresaA, "mismo@correo.com"));

        Usuario duplicado = nuevoUsuario(empresaB, "mismo@correo.com");

        assertThatThrownBy(() -> usuarioRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}