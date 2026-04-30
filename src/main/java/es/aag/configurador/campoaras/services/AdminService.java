package es.aag.configurador.campoaras.services;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.aag.configurador.campoaras.dto.UserGetDTO;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;

/**
 * Servicio encargado de las acciones de administración
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@Service
public class AdminService 
{
	private final Logger log = LogManager.getLogger();
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IRolRepository rolRepo;
	
	@Autowired
	private EncryptorService encryptor;
	

	public AdminService()
	{
		
	}
	
	/**
	 * Metodo que devuelve los usuarios registrados en la app, dependiendo del rol se devolverán determinados usuarios
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 */
	public List<UserGetDTO> getUsers(String rol,String seguridad,String usrToken) throws CPException
	{
		List<Usuario> usuarios = this.userRepo.findAll();
		List<UserGetDTO> response = new LinkedList<UserGetDTO>();
		
		if(!rol.equals(CPConstants.SUPADMIN_ROLE) &&  !rol.equals(CPConstants.ADMIN_ROLE))
		{
			log.warn("[AVISO] -- /get-users -- {} Ha intentado obtener informacion de los usuarios con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		for(Usuario user:usuarios)
		{
			UserGetDTO dto = new UserGetDTO(user.getUuid(),this.encryptor.decrypt(user.getEmail()),this.encryptor.decrypt(user.getUsername()),user.getDescuento(),user.getComercial(),user.getAcceso(),user.getRol().getNombre(),!user.getRol().getNombre().equals(CPConstants.VER_ROLE));
			
			if(!user.getRol().getNombre().equals(CPConstants.SUPADMIN_ROLE))
			{
				response.add(dto);
			}
			else if(rol.equals(CPConstants.SUPADMIN_ROLE))
			{
				response.add(dto);
			}
		}
		
		log.info("[ADMIN] -- /get-users -- {} Ha solicitado obtener una lista de usuarios con el rol {} -- {}",usrToken,rol,seguridad);
		
		return response;
	}
	
	/**
	 * Metodo que verifica o niega (dependiendo del parametro verify) a un usuario que se ha registrado en la app como VERIFICATION, si se niega el acceso el usuario registrado se borra
	 * @param uuid
	 * @param verify
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @throws CPException
	 */
	public void verificateUser(String uuid,boolean verify,String rol,String seguridad,String usrToken) throws CPException
	{
		if(!rol.equals(CPConstants.SUPADMIN_ROLE) &&  !rol.equals(CPConstants.ADMIN_ROLE))
		{
			log.warn("[AVISO] -- /verificate -- {} Ha intentado obtener informacion de los usuarios con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		Optional<Usuario> usuarioOpt = this.userRepo.findById(uuid);
		
		if(!usuarioOpt.isPresent())
		{
			log.warn("[AVISO] -- /verificate -- {} Ha intentado verificar un usuario inexistente con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Usuario no encontrado");
		}
		
		Usuario usuario = usuarioOpt.get();
		
		if(!usuario.getRol().getNombre().equals(CPConstants.VER_ROLE))
		{
			log.warn("[AVISO] -- /verificate -- {} Ha intentado verificar a un usuario que ya está verificado con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		if(verify)
		{
			usuario.setVerificado(true);
			usuario.setRol(this.rolRepo.findByNombre(CPConstants.CLIENTE_ROLE));
			
			log.info("[ADMIN] -- /verificate -- {} Ha verificado al usuario {} con un permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
			
			this.userRepo.save(usuario);		
		}
		else
		{
			log.info("[ADMIN] -- /verificate -- {} Ha negado el acceso a un usuario con un permiso de {} -- {}",usrToken,rol,seguridad);
			
			this.userRepo.delete(usuario);
		}
	}
	
	/**
	 * Metodo que elimina un usuario por su id
	 * @param uuid
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 */
	public void deleteUser(String uuid,String rol,String seguridad,String usrToken) throws CPException
	{
		if(!rol.equals(CPConstants.SUPADMIN_ROLE) &&  !rol.equals(CPConstants.ADMIN_ROLE))
		{
			log.warn("[AVISO] -- /verificate -- {} Ha intentado obtener informacion de los usuarios con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		Optional<Usuario> usuarioOpt = this.userRepo.findById(uuid);
		
		if(!usuarioOpt.isPresent())
		{
			log.warn("[AVISO] -- /verificate -- {} Ha intentado eliminar un usuario inexistente con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Usuario no encontrado");
		}
		
		Usuario usuario = usuarioOpt.get();
		
		if((usuario.getRol().getNombre().equals(CPConstants.SUPADMIN_ROLE) || usuario.getRol().getNombre().equals(CPConstants.ADMIN_ROLE)) && rol.equals(CPConstants.ADMIN_ROLE))
		{
			log.warn("[AVISO] -- /del-user -- {} Ha intentado eliminar al usuario {} siendo {} con permiso de {} -- {}",usrToken,usuario.getUSRToken(),usuario.getRol().getNombre(),rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		log.info("[ADMIN] {} Ha eliminado al usuario {} de la app con permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
		
		this.userRepo.delete(usuario);
	}
	/**
	 * Metodo que actualiza un usuario, solo un ADMINISTRADOR o SUPERADMINISTRADOR tiene el permiso para actualizarlo
	 * @param uuid
	 * @param body
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @throws CPException
	 */
	public void updateUser(String uuid, UserGetDTO body,String rol, String seguridad, String usrToken) throws CPException
	{
		
		if(!rol.equals(CPConstants.SUPADMIN_ROLE) &&  !rol.equals(CPConstants.ADMIN_ROLE))
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha intentado obtener informacion de los usuarios con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		Optional<Usuario> usuarioOpt = this.userRepo.findById(uuid);
		
		if(!usuarioOpt.isPresent())
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha intentado actualizar un usuario inexistente con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Usuario no encontrado");
		}
		
		Usuario usuario = usuarioOpt.get();
		
		if((usuario.getRol().getNombre().equals(CPConstants.SUPADMIN_ROLE) || usuario.getRol().getNombre().equals(CPConstants.ADMIN_ROLE)) && rol.equals(CPConstants.ADMIN_ROLE))
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha intentado actualizar al usuario {} siendo {} con permiso de {} -- {}",usrToken,usuario.getUSRToken(),usuario.getRol().getNombre(),rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		if(body.getUsername()==null || body.getComercial()==null || body.getRol() == null)
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha introducido datos nulos para actualizar al usuario {} a actualizar con permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		if(body.getUsername().isBlank() || body.getDescuento()<0 || body.getRol().isBlank())
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha introducido datos invalidos para actualizar al usuario {} con permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		usuario.setUsername(this.encryptor.encrypt(body.getUsername()));
		usuario.setComercial(body.getComercial());
		usuario.setDescuento(body.getDescuento());
		usuario.setRol(this.rolRepo.findByNombre(body.getRol()));
		
		if(usuario.getRol().getNombre().equals(CPConstants.VER_ROLE))
		{
			usuario.setVerificado(false);
		}
		
		log.info("[ADMIN] -- /upt-user -- {} Ha actualizado al usuario {} con permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
		
		this.userRepo.save(usuario);		
	}
}
