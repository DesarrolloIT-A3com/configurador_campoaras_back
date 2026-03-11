package es.aag.configurador.campoaras.utils;

import es.aag.configurador.campoaras.configurations.DotEnvInitializer;
import io.github.cdimascio.dotenv.Dotenv;

public final class CPConstants 
{
	// ROLES
	public final static String SUPADMIN_ROLE = "SUPERADMIN";
	public final static String ADMIN_ROLE = "ADMIN";
	public final static String CLIENTE_ROLE = "CLIENTE";
	public final static String VER_ROLE = "VERIFICATION";
	
	// RUTAS
    public static final String ENV_PATH = "./src/main/resources/.env";
    public static final String [] ROUTES_SWAGGER = {"/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**","/v3/api-docs.yaml","/openapi/**"};
    public static final String [] ROUTES_JWT = {"/v1/auth/login"};
    
    // VARIABLES DE .ENV
    public static final Dotenv dotenv = DotEnvInitializer.path.isEmpty() ?  
    		Dotenv.configure()
            .filename(ENV_PATH.split("/")[4])
            .load()
		:
			Dotenv.configure()
			.directory(ENV_PATH)
            .filename(ENV_PATH.split("/")[4])
            .load();
    
    // SUPER USUARIO (TEMPORAL)
    public final static String[] ADMIN_NAME = {dotenv.get("ADMIN_NAME")};
    public final static String[] ADMIN_PASS = {dotenv.get("ADMIN_PASS")};
    public static final String ADMIN_MAIL = dotenv.get("ADMIN_MAIL");

    
    
    // CABECERAS
    public final static String[] IP_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA"
        };
    
    public final static String CONTENT_TYPE = "Content-Type";

    
    
}
