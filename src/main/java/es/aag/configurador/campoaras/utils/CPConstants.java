package es.aag.configurador.campoaras.utils;

import java.util.HashMap;
import java.util.Map;

import es.aag.configurador.campoaras.configurations.DotEnvInitializer;
import io.github.cdimascio.dotenv.Dotenv;

public final class CPConstants 
{
	// ROLES
	public final static String SUPADMIN_ROLE = "SUPERADMIN";
	public final static String ADMIN_ROLE = "ADMIN";
	public final static String COMERCIAL_ROLE = "COMERCIAL";
	public final static String CLIENTE_ROLE = "CLIENTE";
	public final static String VER_ROLE = "VERIFICATION";
	
	// RUTAS
    public static final String ENV_PATH = "./src/main/resources/.env";
    public static final String IMG_PATH = "./src/main/resources/imgs";
    public static final String [] ROUTES_SWAGGER = {"/swagger-ui.html","/swagger-ui/**","/v3/api-docs/**","/v3/api-docs.yaml","/openapi/**"};
    public static final String [] ROUTES_JWT = {"/v1/auth/login","/v1/auth/register","/v1/auth/verify/**","/v1/auth/forget-password/**","/v1/auth/reset-password/**"};
    
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
    
    // FUNCIONES PRIVADAS
    public final static Map<String,Boolean> FILL_CAPABILITIES()
    {
    	Map<String,Boolean> map = new HashMap<String, Boolean>();
    	map.put("general_access", false);
    	map.put("admin_panel", false);
    	map.put("managment", false);
    	map.put("users_read", false);
    	map.put("users_write", false);
    	map.put("logs_read", false);
    	map.put("admin_read", false);
    	map.put("admin_write", false);
    	
    	return map;
    }
    
    // VALORES
    public static final String MAP_DEFAULT_VALUE = "CP-novalue";
    
    	// Se usan para métodos que soporten distintos metodos HTTP ejemplo manageProducts en ManagmentService
    
    public static final String POST = "POST";
    public static final String GET = "GET";
    public static final String PATCH = "PATCH";
    public static final String DELETE = "DELETE";

    
    
}
