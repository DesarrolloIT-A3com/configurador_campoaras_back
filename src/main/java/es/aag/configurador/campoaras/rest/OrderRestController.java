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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.OrderService;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/v1/order")
public class OrderRestController 
{
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IRolRepository rolRepo;
	
	@Autowired
	private OrderService service;
	
	private final GeneralSecurity security;
	
	public OrderRestController()
	{
		this.security = new GeneralSecurity();
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/producto-configurado",produces = "application/json")
	public ResponseEntity<?> getSelecciones(@RequestParam(value = "uuid",required=false)final String uuid,
			                                @RequestParam(value = "isEnd",required=false) final Boolean isEnd,
			                                HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/producto-configurado", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/producto-configurado", usuario.getUSRToken());
			
			List<ResponseSeleccion> response =  this.service.getSelecciones(isEnd, uuid, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /producto-configurado -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/producto-configurado/{uuid}")
	public ResponseEntity<?> patchSeleccion (@PathVariable(value = "uuid",required = true)final String uuid,
											 HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/producto-configurado", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/producto-configurado", usuario.getUSRToken());
			
			this.service.deleteSeleccion(uuid, usuario.getRol().getNombre(), seguridad, seguridad);
			
			return ResponseEntity.status(204).build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /producto-configurado -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
		
}
