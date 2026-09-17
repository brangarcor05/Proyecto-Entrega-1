package co.javeriana.dw.proyecto.dto.empresa;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmpresaRequest {

    @NotBlank(message = "El nombre de la empresa es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El RUC es obligatorio")
    @Size(max = 20)
    private String rut;

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200)
    private String razonSocial;

    @NotBlank(message = "El correo de contacto es obligatorio")
    @Email(message = "El correo de contacto no es válido")
    private String email;

    @Size(max = 20)
    private String telefono;

    @Size(max = 255)
    private String direccion;

    @Size(max = 50)
    private String sector;

    @NotBlank(message = "El nombre del representante/administrador es obligatorio")
    @Size(max = 100)
    private String adminNombre;

    @NotBlank(message = "La contraseña del administrador es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String adminPassword;
}