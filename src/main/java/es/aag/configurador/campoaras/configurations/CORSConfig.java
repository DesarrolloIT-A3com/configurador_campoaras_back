package es.aag.configurador.campoaras.configurations;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CORSConfig 
{
	private final Logger log = LogManager.getLogger();
	
	@Value("${url-cors}")
	private String urlCors;
	
	@Bean
	public CorsFilter corsFilter()
	{
		log.info("[ADMIN] Cargando configuracion de CORS");
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(urlCors));
		config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setAllowCredentials(true);
		config.setExposedHeaders(List.of("Set-Cookie"));
		
		UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);	
	}
}
