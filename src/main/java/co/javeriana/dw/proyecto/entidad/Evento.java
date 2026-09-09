package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "evento")
@Getter
@Setter
@NoArgsConstructor
public class Evento extends NodoProceso {

    @Column(nullable = false)
    private String tipo; // INICIO / FIN
}