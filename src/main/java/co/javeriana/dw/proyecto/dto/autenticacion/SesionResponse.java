package co.javeriana.dw.proyecto.dto.autenticacion;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesionResponse {
    private Long usuarioId;
    private String nombre;
    private String email;
    private RolUsuario rolUsuario;
    private Long empresaId;
    private String empresaNombre;
}