package co.javeriana.dw.proyecto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SeguridadConfig {

    /** Cifrado de contrasenas de HU-03: nunca se guardan en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuracion provisional para la primera entrega. Sin este bean, Spring Security
     * aplica su cadena por defecto y exige usuario y contrasena en todos los endpoints,
     * lo que impide probar la API.
     *
     * El control de acceso real (autenticacion por sesion y restriccion por rol) es de la
     * entrega final; por ahora los permisos se validan en la capa de servicio.
     */
    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(peticiones -> peticiones.anyRequest().permitAll());
        return http.build();
    }
}
