package co.javeriana.dw.proyecto.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Configuration;

import co.javeriana.dw.proyecto.dto.arco.ArcoResponse;
import co.javeriana.dw.proyecto.dto.historial.HistorialResponse;
import co.javeriana.dw.proyecto.dto.permisopool.PermisoPoolResponse;
import co.javeriana.dw.proyecto.dto.pool.PoolResponse;
import co.javeriana.dw.proyecto.dto.proceso.ProcesoResponse;
import co.javeriana.dw.proyecto.entidad.Arco;
import co.javeriana.dw.proyecto.entidad.Historial;
import co.javeriana.dw.proyecto.entidad.PermisoPool;
import co.javeriana.dw.proyecto.entidad.Pool;
import co.javeriana.dw.proyecto.entidad.Proceso;

/**
 * Mapeos de procesos, pools, arcos, historial y permisos.
 *
 * Estos DTO son records, asi que no tienen setters y ModelMapper no puede armarlos
 * campo por campo: se registran convertidores. Cada convertidor delega en el metodo
 * desde() del propio DTO para no repetir la construccion en dos lugares.
 */
@Configuration
public class ProcesoMapperConfig {

    public ProcesoMapperConfig(ModelMapper mapper) {

        // El proceso se mapea como si lo consultara su empresa propietaria. Cuando
        // llega compartido desde otra organizacion, el servicio ajusta soloLectura
        // con conSoloLectura(), porque ModelMapper solo conoce el origen.
        mapper.createTypeMap(Proceso.class, ProcesoResponse.class)
                .setConverter(context -> {
                    Proceso proceso = context.getSource();
                    return ProcesoResponse.desde(proceso, proceso.getEmpresa().getId());
                });

        mapper.createTypeMap(Pool.class, PoolResponse.class)
                .setConverter(context -> PoolResponse.desde(context.getSource()));

        mapper.createTypeMap(Arco.class, ArcoResponse.class)
                .setConverter(context -> ArcoResponse.desde(context.getSource()));

        mapper.createTypeMap(Historial.class, HistorialResponse.class)
                .setConverter(context -> HistorialResponse.desde(context.getSource()));

        mapper.createTypeMap(PermisoPool.class, PermisoPoolResponse.class)
                .setConverter(context -> PermisoPoolResponse.desde(context.getSource()));
    }
}
