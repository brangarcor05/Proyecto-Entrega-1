package co.javeriana.dw.proyecto.config;

import co.javeriana.dw.proyecto.security.JwtAuthenticationFilter;
import co.javeriana.dw.proyecto.security.JwtService;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity // habilita @PreAuthorize en los controllers
public class SeguridadConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

   @Bean
public SecurityFilterChain filterChain(HttpSecurity http, JwtService jwtService) throws Exception {
    http
        .cors(cors -> cors.configurationSource(corsConfigurationSource())) //FORZAR CORS
        .csrf(csrf -> csrf.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a
            // Respuesta de error interna de Spring Boot (/error). Cuando un endpoint
            // lanza una excepcion, Spring reenvia la peticion a /error sin el token; si
            // esa ruta exige autenticacion, el 400/500 real llega al cliente como 401.
            // No abre ningun endpoint: solo deja que se entregue el error verdadero.
            .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
            // Registro de empresa (HU-01) y los tres endpoints de sesion (HU-03)
            .requestMatchers(HttpMethod.POST, "/api/empresas",
                    "/api/auth/login", "/api/auth/refresh", "/api/auth/logout").permitAll()
            // Documentacion de la API (Swagger / OpenAPI)
            .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() //IMPORTANTE: Permitir preflight
            .anyRequest().authenticated())
        .exceptionHandling(e -> e.authenticationEntryPoint(
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .addFilterBefore(new JwtAuthenticationFilter(jwtService),
            UsernamePasswordAuthenticationFilter.class);
    return http.build();
}

@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOriginPatterns(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
}
}
