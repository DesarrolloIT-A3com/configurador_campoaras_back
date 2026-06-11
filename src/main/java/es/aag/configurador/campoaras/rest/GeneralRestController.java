package es.aag.configurador.campoaras.rest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import es.aag.configurador.campoaras.dto.OrderDTO;
import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.dto.UserGetDTO;
import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IBulkProductosUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IProductoConfiguradoRepository;
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
	private IBulkProductosUsuarioRepository bulkRepo;
	
	@Autowired
	private IProductoConfiguradoRepository seleccionRepo;
	
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
			
			UserGetDTO response = new UserGetDTO(usuario.getUuid(), this.encryptor.decrypt(usuario.getEmail()), this.encryptor.decrypt(usuario.getUsername()),null,usuario.getDescuento(), usuario.getSegundoDescuento(), null , null, null, usuario.isVerificado());
			
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
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/configure/{uuid}")
	public ResponseEntity<?> endConfigure(@PathVariable(value = "uuid",required = true) final String uuid,
			HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configure", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/configure", usuario.getUSRToken());

			Optional<BulkProductosUsuario> bulkOpt = this.bulkRepo.findById(uuid);
			
			if(!bulkOpt.isPresent())
			{
				log.warn("[AVISO] -- /configure -- {} Ha tratado de finalizar una configuracion inexistente con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),usuario.getUSRToken());
				throw new CPException(404,"Datos inexistentes");
			}
			
			BulkProductosUsuario bulk = bulkOpt.get();
			
			bulk.setEnd(true);
			bulk.setFecha(LocalDateTime.now());
			this.bulkRepo.save(bulk);
			this.bulkRepo.flush();
			
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
			
			log.error("[ERROR] -- /configure -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/configure",consumes="application/json")
	public ResponseEntity<?> patchObservacionConfigure(@RequestBody(required = true) final Map<String,String> body,
			HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configure", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/configure", usuario.getUSRToken());
			
			String uuid = body.getOrDefault("uuid", CPConstants.MAP_DEFAULT_VALUE);
			String observacion = body.getOrDefault("observaciones", CPConstants.MAP_DEFAULT_VALUE);
			
			if(uuid==null || observacion==null)
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado actualizar una observación de una seleccion con un uuid u observación nulos con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
				throw new CPException(400,"Datos invalidos");
			}
			
			if(uuid.equals(CPConstants.MAP_DEFAULT_VALUE))
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado actualizar una observación de una seleccion con un uuid inexistente con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
				throw new CPException(400,"Datos invalidos");
			}
			
			if(observacion.equals(CPConstants.MAP_DEFAULT_VALUE))
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado actualizar una observación de una seleccion con el campo observación vacío con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
				throw new CPException(400,"Datos invalidos");
			}
			
			Optional<ProductoConfigurado> seleccionOpt = this.seleccionRepo.findById(uuid);
			
			if(!seleccionOpt.isPresent())
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado actualizar una observación de una seleccion inexistente con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
				throw new CPException(404,"Datos inexistentes");
			}
			
			ProductoConfigurado seleccion = seleccionOpt.get();
			
			seleccion.setObservaciones(this.encryptor.encrypt(observacion));
			
			this.seleccionRepo.save(seleccion);
			this.seleccionRepo.flush();
			
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
	
	@RequestMapping(method = RequestMethod.POST,value = "/order-proposal/send",consumes="multipart/form-data")
	public ResponseEntity<?> sendOrder(@RequestPart(name="pdf",required = true) final MultipartFile img,
									   @RequestPart(name="uuid",required = true) final String uuid,
									   HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/order-proposal", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/order-proposal", usuario.getUSRToken());
			
			this.security.validatePdf(img, "/order-proposal/send", usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());

			this.orderService.sendPedido(img, usuario, uuid, seguridad);
			
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
						
			log.error("[ERROR] -- /order-proposal/send -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/scrapping/color",produces = "application/json")
	public ResponseEntity<?> getColor(@RequestParam(value="code",required=true)final String code,
									  @RequestParam(value="type",required=true)final String type,
									  HttpServletRequest request,Authentication authentication)
	
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/scrapping/color", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/order-proposal", usuario.getUSRToken());
			
			String color = null;
			String url = "";
			
			if(type.equals(CPConstants.RAL_VALUE))
			{
				url = CPConstants.URL_RAL+"?q=" + URLEncoder.encode(code,StandardCharsets.UTF_8);
			}
			else if(type.equals(CPConstants.NCS_VALUE))
			{
				url = CPConstants.URL_NCS+"?q=" + URLEncoder.encode(code,StandardCharsets.UTF_8);
			}
			else
			{
				log.warn("[AVISO] -- /scrapping/color -- {} Ha introducido datos invalidos para obtener el color pedido con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
				throw new CPException(400,"Datos invalidos");
			}
			
			Document doc = Jsoup.connect(url)
					.userAgent("Mozzilla/5.0")
					.timeout(10000)
					.get();
			
			Elements colores = doc.select(".colors");
			
			if(colores.size()>0)
			{
				for(Element item:colores)
				{
					Element ul = item.getElementsByTag("ul").get(0);
					
					Elements listItem = ul.getElementsByTag("li");
					
					for(Element li:listItem)
					{
						Element liChild = li.getElementsByTag("a").get(0);
						
						String valor = liChild.text();
													
						if(valor.equals(type+" "+code))
						{
							String styles = liChild.attr("style");
							String[] propiedades = styles.split(";");
							
							for(String style:propiedades)
							{
								if(style.trim().split(":")[0].equals("background-color"))
								{
									color = style.trim().split(":")[1];
									break;
								}
							}
						}
						
						if(color!=null)
						{
							break;
						}
					}
					
					if(color!=null)
					{
						break;
					}
				}
			}
			
			Map<String,String> response = new HashMap<String, String>();
			response.put("color", color);
			
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
						
			log.error("[ERROR] -- /scrapping/color -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		

		}
	}
	
}
