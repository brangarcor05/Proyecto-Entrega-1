package co.javeriana.dw.proyecto.dto.arco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SincronizarArcoRequest(

        @NotBlank(message = "El identificador visual del arco es obligatorio")
        @Size(max = 100, message = "El identificador visual no puede superar los 100 caracteres")
        String canvasId,

        @NotBlank(message = "El identificador visual del origen es obligatorio")
        @Size(max = 100, message = "El identificador visual del origen no puede superar los 100 caracteres")
        String origenCanvasId,

        @NotBlank(message = "El identificador visual del destino es obligatorio")
        @Size(max = 100, message = "El identificador visual del destino no puede superar los 100 caracteres")
        String destinoCanvasId,

        @Size(max = 150, message = "La etiqueta no puede superar los 150 caracteres")
        String etiqueta,

        @Size(max = 500, message = "La condicion no puede superar los 500 caracteres")
        String condicion) {
}
