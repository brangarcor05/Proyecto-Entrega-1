package co.javeriana.dw.proyecto.dto.comun;

import co.javeriana.dw.proyecto.entidad.CampoDato;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Un campo del contenido de un mensaje o notificacion: nombre y tipo de dato. */
public record CampoDatoDto(

        @NotBlank(message = "El nombre del campo es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de dato es obligatorio")
        @Size(max = 50, message = "El tipo de dato no puede superar los 50 caracteres")
        String tipoDato) {

    public static CampoDatoDto desde(CampoDato campo) {
        return new CampoDatoDto(campo.getNombre(), campo.getTipoDato());
    }

    public CampoDato aEntidad() {
        return new CampoDato(nombre, tipoDato);
    }
}
