package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "gateway")
@Getter
@Setter
@NoArgsConstructor
public class Gateway extends NodoProceso {

    @Column(nullable = false)
    private String tipo; // exclusivo / paralelo / inclusivo — se define en HU-14
}