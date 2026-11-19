package co.javeriana.dw.proyecto.service;

import co.javeriana.dw.proyecto.dto.autenticacion.LoginRequest;
import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.entidad.Usuario;
import co.javeriana.dw.proyecto.exception.CredencialesInvalidasException;
import co.javeriana.dw.proyecto.exception.SesionInvalidaException;
import co.javeriana.dw.proyecto.repository.UsuarioRepository;
import co.javeriana.dw.proyecto.security.JwtService;
import co.javeriana.dw.proyecto.security.RefreshTokenService;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Inicio de sesión, renovación y cierre de sesión (HU-03).
 *
 * Flujo:
 *  1. login: correo + contraseña → access token (corto) + refresh token (largo).
 *  2. El frontend usa el access token en cada petición.
 *  3. Cuando el access token vence (401), el frontend llama a refresh con el refresh
 *     token y recibe un par nuevo. El rol y la empresa se leen otra vez de la base de
 *     datos, así que un cambio de rol se refleja en la siguiente renovación.
 *  4. logout revoca el refresh token; el access token muere solo en pocos minutos.
 */
@Service
public class AutenticacionService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AutenticacionService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder,
                                ModelMapper modelMapper, JwtService jwtService,
                                RefreshTokenService refreshTokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public SesionResponse iniciarSesion(LoginRequest request) {
        // HU-03: el mismo error para correo inexistente, contraseña mala o usuario
        // inactivo, para no revelar si el correo está registrado.
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(CredencialesInvalidasException::new);
        if (!usuario.isActivo() || !passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        return crearSesion(usuario, refreshTokenService.emitir(usuario));
    }

    /** Mismo noRollbackFor que RefreshTokenService.rotar: la revocación por robo debe quedar guardada. */
    @Transactional(noRollbackFor = SesionInvalidaException.class)
    public SesionResponse renovar(String refreshToken) {
        RefreshTokenService.Rotacion rotacion = refreshTokenService.rotar(refreshToken);
        return crearSesion(rotacion.usuario(), rotacion.nuevoToken());
    }

    @Transactional
    public void cerrarSesion(String refreshToken) {
        refreshTokenService.revocar(refreshToken);
    }

    private SesionResponse crearSesion(Usuario usuario, String refreshToken) {
        SesionResponse sesion = modelMapper.map(usuario, SesionResponse.class);
        sesion.setTokenType("Bearer");
        sesion.setAccessToken(jwtService.generarAccessToken(usuario));
        sesion.setExpiresIn(jwtService.getExpiracionSegundos());
        sesion.setRefreshToken(refreshToken);
        sesion.setRefreshExpiresIn(refreshTokenService.getDuracionSegundos());
        return sesion;
    }
}
