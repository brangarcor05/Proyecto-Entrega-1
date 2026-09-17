package co.javeriana.dw.proyecto.dto.empresa;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaResponse {
    private Long id;
    private String nombre;
    private String ruc;
    private String razonSocial;
    private String email;
    private String telefono;
    private String direccion;
    private String sector;
    private String estado;
    private LocalDateTime fechaRegistro;
    private Long adminUsuarioId;
}