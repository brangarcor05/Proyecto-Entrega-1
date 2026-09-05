package co.javeriana.dw.proyecto.dto.usuario;

import com.fasterxml.jackson.annotation.JsonProperty;

import co.javeriana.dw.proyecto.entidad.RolUsuario;

public record CrearUsuarioRequest(
        String nombre,
        String email,
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String password,
        RolUsuario rol) {

    @Override
    public String toString() {
        return "CrearUsuarioRequest[nombre=" + nombre
                + ", email=" + email
                + ", password=***"
                + ", rol=" + rol + "]";
    }
}
