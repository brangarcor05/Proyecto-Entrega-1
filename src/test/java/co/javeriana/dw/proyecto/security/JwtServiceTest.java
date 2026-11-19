package co.javeriana.dw.proyecto.security;

import co.javeriana.dw.proyecto.entidad.Empresa;
import co.javeriana.dw.proyecto.entidad.RolUsuario;
import co.javeriana.dw.proyecto.entidad.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/** Token de corta duración (access token) de HU-03. */
class JwtServiceTest {

    private static final String SECRETO = "Y2xhdmUtZGUtcHJ1ZWJhcy1zb2xvLXBhcmEtdGVzdHMtMzJieXRlcyEhIQ==";

    private final JwtService jwtService = new JwtService(SECRETO, 15);

    private Usuario usuario() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);
        Usuario usuario = new Usuario();
        usuario.setId(3L);
        usuario.setEmail("juan@acme.com");
        usuario.setRolUsuario(RolUsuario.EDITOR);
        usuario.setEmpresa(empresa);
        return usuario;
    }

    @Test
    void elAccessTokenLlevaUsuarioEmpresaYRol() {
        String token = jwtService.generarAccessToken(usuario());

        UsuarioAutenticado autenticado = jwtService.validar(token).orElseThrow();

        assertThat(autenticado.id()).isEqualTo(3L);
        assertThat(autenticado.empresaId()).isEqualTo(1L);
        assertThat(autenticado.email()).isEqualTo("juan@acme.com");
        assertThat(autenticado.rol()).isEqualTo(RolUsuario.EDITOR);
    }

    @Test
    void elAccessTokenDuraLoConfigurado() {
        assertThat(jwtService.getExpiracionSegundos()).isEqualTo(15 * 60);
    }

    @Test
    void rechazaUnTokenAlterado() {
        String token = jwtService.generarAccessToken(usuario());
        String alterado = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThat(jwtService.validar(alterado)).isEmpty();
    }

    @Test
    void rechazaUnTokenVencido() {
        String vencido = Jwts.builder()
                .subject("juan@acme.com")
                .claim("tipo", "access")
                .claim("usuarioId", 3L)
                .claim("empresaId", 1L)
                .claim("rol", "EDITOR")
                .issuedAt(Date.from(Instant.now().minusSeconds(3600)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRETO)))
                .compact();

        assertThat(jwtService.validar(vencido)).isEmpty();
    }

    @Test
    void rechazaUnJwtQueNoEsDeAcceso() {
        // Firmado con la misma clave pero sin tipo "access": no debe abrir la API.
        String otroTipo = Jwts.builder()
                .subject("juan@acme.com")
                .claim("tipo", "refresh")
                .claim("usuarioId", 3L)
                .claim("empresaId", 1L)
                .claim("rol", "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRETO)))
                .compact();

        assertThat(jwtService.validar(otroTipo)).isEmpty();
    }
}
