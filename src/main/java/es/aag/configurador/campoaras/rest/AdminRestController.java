package es.aag.configurador.campoaras.rest;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import es.aag.configurador.campoaras.dto.UserGetDTO;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.AdminService;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador encargado de las acciones de administración relacionado con gestión de usuarios
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@RestController
@RequestMapping(value = "/v1/admin")
public class AdminRestController 
{
	private final Logger log = LogManager.getLogger();
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IRolRepository rolRepo;
	
	@Autowired
	private AdminService adminService;
	
	private final GeneralSecurity security;
	
	public AdminRestController()
	{
		this.security = new GeneralSecurity();
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/get-users",produces = "application/json")
	public ResponseEntity<?> getUsers(HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/get-users", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/get-users", usuario.getUSRToken());
			
			List<UserGetDTO> response = this.adminService.getUsers(usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.ok().body(response);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /get-users -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/verificate/{userId}")
	public ResponseEntity<?> verificateUser(@PathVariable(value = "userId",required = true)final String userId,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/verificate", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/verificate", usuario.getUSRToken());
			
			this.adminService.verificateUser(userId, true, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.ok().build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /verificate -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/verificate/{userId}")
	public ResponseEntity<?> unverificateUser(@PathVariable(value = "userId",required = true)final String userId,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/verificate", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/verificate", usuario.getUSRToken());
			
			this.adminService.verificateUser(userId, false,usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.ok().build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /verificate -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/del-user/{userId}")
	public ResponseEntity<?> deleteUser (@PathVariable(value = "userId",required = true) String uuid,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/del-user", seguridad);
			
			if(usuario.getUuid().equals(uuid))
			{
				log.warn("[AVISO] -- /del-user -- {} ha intentado eliminarse a si mismo con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
				throw new CPException(403,"No tienes permiso");
			}
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/del-user", usuario.getUSRToken());
			
			this.adminService.deleteUser(uuid, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.ok().build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /del-user -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/upt-user/{userId}",consumes = "application/json")
	public ResponseEntity<?> updateUser (@PathVariable(value = "userId",required = true) String uuid,
			@RequestBody(required = true) UserGetDTO body,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/upt-user", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/verificate", usuario.getUSRToken());
			
			this.adminService.updateUser(uuid, body, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.ok().build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /del-user -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
}
