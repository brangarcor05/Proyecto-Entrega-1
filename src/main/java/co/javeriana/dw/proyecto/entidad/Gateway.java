package co.javeriana.dw.proyecto.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "gateway")
@Getter
@Setter
@NoArgsConstructor
public class Gateway extends NodoProceso {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoGateway tipo;
}