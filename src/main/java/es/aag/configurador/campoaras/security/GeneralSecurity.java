package es.aag.configurador.campoaras.security;

import java.time.LocalDateTime;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.servlet.http.HttpServletRequest;

public class GeneralSecurity 
{
	private Logger log = LogManager.getLogger();
	
	
	public GeneralSecurity()
	{
		
	}

	/**
	 * Método para obtener la ip del usuario que realiza un movimiento en el servidor
	 * @param request
	 * @return Valor de la ip
	 */
	public String getClientIPAddress(HttpServletRequest request)
	{
		for(String header : CPConstants.IP_HEADERS)
		{
			String ip = request.getHeader(header);
			
			if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) 
			{
	            // Para X-Forwarded-For que puede tener múltiples IPs
	            if (header.equals("X-Forwarded-For")) 
	            {
	                return ip.split(",")[0].trim();
	            }
	            return ip;
	        }
		}
		
		// En caso de no encontrar ninguna ip
		return request.getRemoteAddr();
	}
	
	/**
	 * Método para obtener información relacionada con la ip
	 * @param ip
	 * @param request
	 * @param usuario
	 * @param usuarioRepo
	 */
	public String getIpInfo(String ip,HttpServletRequest request)
	{
		String info = "[SEGURIDAD] IP:"+ip+" | ";
		// Informacion confiable y crítica
		info += "Accept-Language:"+request.getHeader("Accept-Language") + " | ";
		info += "REMOTE_ADDR:"+request.getRemoteAddr() + " | ";
		info += "HOST:"+request.getRemoteHost()+ " | ";
		info += "PORT:"+String.valueOf(request.getRemotePort())+ " | ";
		
		
		// Información de análisis
		info += "Referer:"+request.getHeader("Referer")+ " | ";
	    info += "Origin:"+request.getHeader("Origin")+" | ";
	    info += "X-Forwarded-For:"+request.getHeader("X-Forwarded-For")+ " | ";
	    info += "X-Real-IP:"+request.getHeader("X-Real-IP")+ " | ";
	    info += "Content-Length:"+request.getHeader("Content-Length")+ " | ";
		info += "Accept-Encoding:"+request.getHeader("Accept-Encoding")+ " | ";
	    info += "User-Agent:"+request.getHeader("User-Agent")+ " | ";
	    info += "timestamp:"+LocalDateTime.now().toString()+ " | ";
	    
	    return info;
	}
	
	/**
	 * Metodo para verificar que el usuario que ha introducido sus credenciales de sesión + JWT sean válidas
	 * @param userRepo
	 * @param seguridad
	 * @return Usuario autenticado
	 * @throws CPException
	 */
	public Usuario isAuth(IUsuarioRepository userRepo,String seguridad) throws CPException
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	    Optional<Usuario> userOpt = userRepo.findById(authentication.getName());
	    
	    if(!userOpt.isPresent())
	    {
	    	log.info("[AVISO] Se ha tratado de iniciar sesión con un usuario inexistente con un JWT no válido -- {}",seguridad);
	    	throw new CPException(401, "Inicio de sesión inválido");
	    }
		
	    Usuario usuario = userOpt.get();
	    
	    return usuario;
	}
	
	
}
