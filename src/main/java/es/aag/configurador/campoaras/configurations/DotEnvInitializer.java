package es.aag.configurador.campoaras.configurations;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

public class DotEnvInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext>
{
	private static Logger log = LogManager.getLogger();
	public static String path = "";
	
	@Override
	public void initialize(ConfigurableApplicationContext applicationContext)
	{
	    log.info("[ADMIN] Cargando variables de entorno");

	    // En producción las variables ya están en el sistema (export / .service)
	    // .env se carga si existe por lo que el sistema entenderá que está en desarrollo
	    try
	    {
	        Dotenv dotenv = Dotenv.configure()
	                .directory("src/main/resources")
	                .filename(".env")
	                .ignoreIfMissing()   // ← clave: no falla si no existe
	                .load();
	        dotenv.entries().forEach(entry -> {
	            // Solo sobreescribe si el sistema NO tiene ya la variable
	            if (System.getProperty(entry.getKey()) == null
	                    && System.getenv(entry.getKey()) == null) {
	                System.setProperty(entry.getKey(), entry.getValue());
	            }
	        });
	        log.info("[ADMIN] .env cargado desde src/main/resources");
	    }
	    catch (Exception ex)
	    {
	        log.info("[ADMIN] No se encontró .env local, usando variables del sistema");
	    }
	}

}
