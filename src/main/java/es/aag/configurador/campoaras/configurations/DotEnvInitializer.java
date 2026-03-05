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
		Dotenv dotenv = null;
		
		try
		{
			path = "src/main/resources";
			dotenv = Dotenv.configure()
					.directory(path)
					.filename(".env")
					.load();
		}
		catch(DotenvException ex)
		{
			dotenv = Dotenv.configure()
					.filename(".env")
					.load();
		}
		dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
		
	}

}
