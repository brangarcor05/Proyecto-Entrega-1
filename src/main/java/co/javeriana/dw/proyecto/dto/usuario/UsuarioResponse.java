package co.javeriana.dw.proyecto.dto.usuario;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private RolUsuario rolUsuario;
    private boolean activo;
    private Long empresaId;
    private String empresaNombre;
}
