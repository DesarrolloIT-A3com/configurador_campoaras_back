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

import es.aag.configurador.campoaras.dto.OrderDTO;
import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.dto.UserGetDTO;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.ConfigurationService;
import es.aag.configurador.campoaras.services.EncryptorService;
import es.aag.configurador.campoaras.services.OrderService;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/v1")
public class GeneralRestController 
{
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IRolRepository rolRepo;
	
	@Autowired
	private ConfigurationService service;
	
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private EncryptorService encryptor;
	
	private final GeneralSecurity security;
	
	public GeneralRestController()
	{
		this.security = new GeneralSecurity();
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/user",produces = "application/json")
	public ResponseEntity<?> getUserData (HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/user", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/user", usuario.getUSRToken());
			
			UserGetDTO response = new UserGetDTO(usuario.getUuid(), this.encryptor.decrypt(usuario.getEmail()), this.encryptor.decrypt(usuario.getUsername()), usuario.getDescuento(), usuario.getSegundoDescuento(), null , null, null, usuario.isVerificado());
			
			log.info("[ACCION] -- /user -- {} Ha solicitado sus datos de usuario con un permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
			
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
			
			log.error("[ERROR] -- /user -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/configure",consumes = "application/json")
	public ResponseEntity<?> configureProduct (@RequestBody(required = true) SeleccionDTO seleccion,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configure", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/configure", usuario.getUSRToken());
			
			String uuid = this.service.configureProduct(seleccion, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken(), usuario);
			
			return ResponseEntity.status(201).body(uuid);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /configure -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/configure",produces = "application/json")
	public ResponseEntity<?> getProducts(@RequestParam(value="isEnd",required = false) final Boolean isEnd,
										 @RequestParam(value="uuid",required = false) final String uuid,
			HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configure", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/configure", usuario.getUSRToken());
			
			List<ResponseSeleccion> response = this.service.getSelecciones(isEnd,uuid,usuario, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /configure -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/configure/{uuid}")
	public ResponseEntity<?> delConfigureProduct(@PathVariable(value = "uuid",required = true) final String uuid,
			HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configure", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/configure", usuario.getUSRToken());
			
			this.service.delSeleccion(uuid, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /configure -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
	@RequestMapping(method = RequestMethod.POST,value="/order-proposal",consumes="application/json")
	public ResponseEntity<?> postOrder(@RequestBody(required = true)final OrderDTO order,
			HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/order-proposal", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/order-proposal", usuario.getUSRToken());
			
			this.orderService.postOrder(order, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(201).build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /order-proposal -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/order-proposal",produces="application/json")
	public ResponseEntity<?> getOrder(HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/order-proposal", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/order-proposal", usuario.getUSRToken());
			
			List<OrderDTO> response =  this.orderService.getPedidos(usuario, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /order-proposal -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
		
	}
}
