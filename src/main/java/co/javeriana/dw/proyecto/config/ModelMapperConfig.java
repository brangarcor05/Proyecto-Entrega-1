package co.javeriana.dw.proyecto.config;

import co.javeriana.dw.proyecto.dto.autenticacion.SesionResponse;
import co.javeriana.dw.proyecto.dto.usuario.UsuarioResponse;
import co.javeriana.dw.proyecto.entidad.Usuario;
import org.modelmapper.ModelMapper;
import org.modelmapper.PropertyMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

       
        mapper.addMappings(new PropertyMap<Usuario, UsuarioResponse>() {
            @Override
            protected void configure() {
                map().setEmpresaId(source.getEmpresa().getId());
                map().setEmpresaNombre(source.getEmpresa().getNombre());
            }
        });

        mapper.addMappings(new PropertyMap<Usuario, SesionResponse>() {
            @Override
            protected void configure() {
                map().setUsuarioId(source.getId());
                map().setEmpresaId(source.getEmpresa().getId());
                map().setEmpresaNombre(source.getEmpresa().getNombre());
            }
        });

        return mapper;
    }
}