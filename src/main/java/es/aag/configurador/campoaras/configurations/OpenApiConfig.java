package es.aag.configurador.campoaras.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig 
{
	@Bean
	public OpenAPI customOpenAPI()
	{
		return new OpenAPI()
				.info(new Info()
						.title("Campoaras API")
						.version("1.0.0")
						.description("Documentacion de la API REST para mantenimiento y testeo")
						.contact(new Contact()
								.name("Desarrollo IT")
								.email("desarrolloit@a3com.es")
						)
					 )
					.addServersItem(new Server()
					.url("http://localhost:8080")
					.description("Servidor de desarrollo")
					)
					.components(new Components()
							.addSecuritySchemes("cookieAuth", new SecurityScheme()
									.type(SecurityScheme.Type.APIKEY)
									.in(SecurityScheme.In.COOKIE)
									.name("jwt")
									.description("Token JWT en cookie HttpOnly")
							)
					
					
					)
					.addSecurityItem(new SecurityRequirement()
							.addList("cookieAuth")
					);		
	}
}
