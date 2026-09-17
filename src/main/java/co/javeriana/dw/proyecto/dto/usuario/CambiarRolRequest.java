package co.javeriana.dw.proyecto.dto.usuario;

import co.javeriana.dw.proyecto.entidad.RolUsuario;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambiarRolRequest {

    @NotNull(message = "El nuevo rol de acceso es obligatorio")
    private RolUsuario rolUsuario;
}