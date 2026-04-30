package es.aag.configurador.campoaras.security;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import es.aag.configurador.campoaras.entities.Rol;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IRolRepository;
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
	public Usuario isAuth(IUsuarioRepository userRepo, String endpoint ,String seguridad) throws CPException
	{
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

	    Optional<Usuario> userOpt = userRepo.findById(authentication.getName());
	    
	    if(!userOpt.isPresent())
	    {
	    	log.warn("[AVISO] -- {} -- Se ha tratado de iniciar sesión con un usuario inexistente con un JWT no válido -- {}",endpoint,seguridad);
	    	throw new CPException(401, "Inicio de sesión inválido");
	    }
		
	    Usuario usuario = userOpt.get();
	    
	    return usuario;
	}
	
	/**
	 * Método que establece una jerarquía de permisos sobre acciones que un usuario con determinado rol puede o no puede realizar
	 * @param rolRepo
	 * @param rol
	 * @param capabilitie
	 * @param seguridad
	 * @param endpoint
	 * @param userToken
	 * @throws CPException
	 */
	public void hierarchy(IRolRepository rolRepo, Rol rol, String capabilitie,String seguridad,String endpoint,String userToken) throws CPException
	{
		
		if(rol == null)
		{
			log.warn("[AVISO] -- {} -- Ha intentado acceder con sin rol -- {}",userToken,seguridad);
			throw new CPException(403,"No tienes permiso de acceso");
		}
		
		Rol superRole = rolRepo.findByNombre(CPConstants.SUPADMIN_ROLE);
		Rol adminRole = rolRepo.findByNombre(CPConstants.ADMIN_ROLE);
		Rol clienteRole = rolRepo.findByNombre(CPConstants.CLIENTE_ROLE);
		Rol verRole = rolRepo.findByNombre(CPConstants.VER_ROLE);
		
		switch (capabilitie)
		{
			case CPConstants.SUPADMIN_ROLE:
			{
				if(!rol.equals(superRole))
				{
					log.warn("[AVISO] -- {} -- {} Ha intentado acceder a permisos de {} con un rol de {} -- {}",endpoint,userToken,CPConstants.SUPADMIN_ROLE,rol.getNombre(),seguridad);
					throw new CPException(403,"No tienes permiso de acceso");
				}
				break;
			}
			case CPConstants.ADMIN_ROLE:
			{
				if(!rol.equals(superRole) && !rol.equals(adminRole))
				{
					log.warn("[AVISO] -- {} -- {} Ha intentado acceder a permisos de {} con un rol de {} -- {}",endpoint,userToken,CPConstants.ADMIN_ROLE,rol.getNombre(),seguridad);
					throw new CPException(403,"No tienes permiso de acceso");
				}
				break;
			}
			case CPConstants.CLIENTE_ROLE:
			{
	
				if(!rol.equals(superRole) && !rol.equals(adminRole) && !rol.equals(clienteRole))
				{
					log.warn("[AVISO] -- {} -- {} Ha intentado acceder a permisos de {} con un rol de {} -- {}",endpoint,userToken,CPConstants.CLIENTE_ROLE,rol.getNombre(),seguridad);
					throw new CPException(403,"No tienes permiso de acceso");
				}
				break;
			}
			case CPConstants.VER_ROLE:
			{
				if(!rol.equals(superRole) && !rol.equals(adminRole) && !rol.equals(clienteRole) && !rol.equals(verRole))
				{
					log.warn("[AVISO] -- {} -- {} Ha intentado acceder a permisos de {} con un rol de {} -- {}",endpoint,userToken,CPConstants.VER_ROLE,rol.getNombre(),seguridad);
					throw new CPException(403,"No tienes permiso de acceso");
				}
				break;
			}
			default:
			{
				log.warn("[AVISO] -- {} -- {} Ha intentado acceder con un rol no válido entre la jerarquia definida -- {}",endpoint,userToken,seguridad);
				throw new CPException(403,"No tienes permiso de acceso");
			}
		}
	
	}
	
	/**
	 * Metodo que valida la entrada de imágenes para evitar inclusión de ficheros no deseados
	 * @param file
	 * @param endpoint
	 * @param seguridad
	 * @param userToken
	 * @throws CPException
	 */
	public void validateImg(MultipartFile file,String endpoint,String rol,String seguridad,String userToken) throws CPException
	{
		// FIRMA PNG
		final byte[] PNG_MAGIC  = {(byte)0x89, 0x50, 0x4E, 0x47};
		// FIRMA JPEG
		final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
	
		final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
		
		final Set<String> ALLOWED_MIME_TYPES = Set.of("image/png", "image/jpeg");
		
		if(file.isEmpty())
		{
			log.warn("[AVISO] -- {} -- {} Ha introducido un fichero vacío con permiso de {} -- {}",endpoint,userToken,rol,seguridad);
			throw new CPException(400,"Datos invalidos");
		}
		
		if(file.getSize() > MAX_SIZE_BYTES)
		{
			long size = file.getSize() / 1024 / 1024;
			log.warn("[AVISO] -- {} -- {} Ha introducido un fichero que supera los 5MB de tamaño siendo de tamaño {} MB con permiso de {} -- {}",endpoint,userToken,size,rol,seguridad);
			throw new CPException(400,"Datos invalidos");
		}
		
		String declaredMime = file.getContentType();
		if(declaredMime == null || !ALLOWED_MIME_TYPES.contains(declaredMime))
		{
			log.warn("[AVISO] -- {} -- {} Ha introducido un fichero no permitido con un MIME {} que no es jpg o png con permiso de {} -- {}",endpoint,userToken,declaredMime,rol,seguridad);
			throw new CPException(400,"Datos invalidos");
		}
		
		boolean isPng = false;
		boolean isJpg = false;
		
		try
		{
			InputStream is = file.getInputStream();
			byte[] header = is.readNBytes(4);
			
			
			// Comprobación de las cabeceras del fichero, deben de coincidir con el magic number de un PNG si no coincide da false
			isPng = header.length >= 4 
					&& header[0] == PNG_MAGIC[0] && header[1] == PNG_MAGIC[1]
					&& header[2] == PNG_MAGIC[2] && header[3] == PNG_MAGIC[3];
			
			isJpg = header.length >= 3
		            && header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1]
		            && header[2] == JPEG_MAGIC[2];
			

		}
		catch(IOException ex)
		{
			log.error("[ERROR] -- {} -- {} Ha saltado un error IOException al leer los bytes del fichero -- {}",endpoint,userToken,seguridad);
			isPng = false;
			isJpg = false;
		}
		catch(IndexOutOfBoundsException ex)
		{
			log.warn("[ERROR] -- {} -- {} Ha saltado un error IndexOutOfBoundsException debido a que el fichero no posee el número de bytes necesario (4) para la validación de un magic number -- {}",endpoint,userToken,seguridad);
			isPng = false;
			isJpg = false;
		}
		
		if(!isPng && !isJpg)
		{
			log.warn("[AVISO] -- {} -- {} Ha introducido un fichero que no es una imagen con permiso de {} -- {}",endpoint,userToken,rol,seguridad);
			throw new CPException(400,"Datos invalidos");
		}
	}
	
	
}
