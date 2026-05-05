package es.aag.configurador.campoaras.rest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

import es.aag.configurador.campoaras.dto.ResponseAcabado;
import es.aag.configurador.campoaras.dto.ResponseColor;
import es.aag.configurador.campoaras.dto.ResponseConfiguracion;
import es.aag.configurador.campoaras.dto.ResponseFrente;
import es.aag.configurador.campoaras.dto.ResponseProducto;
import es.aag.configurador.campoaras.dto.ResponseSerie;
import es.aag.configurador.campoaras.entities.Producto;
import es.aag.configurador.campoaras.entities.Serie;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.ConfigurationService;
import es.aag.configurador.campoaras.services.ManagmentService;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Controlador encargado de las acciones de administración relacionado con gestión de productos de la fábrica y configuraciones
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@RestController
@RequestMapping(value = "/v1/managment")
public class ManagmentRestController 
{
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IRolRepository rolRepo;
	
	@Autowired
	private ManagmentService service;
	
	@Autowired
	private ConfigurationService configService;
	
	private final GeneralSecurity security;
	
	public ManagmentRestController()
	{
		this.security = new GeneralSecurity();
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/products",consumes = "multipart/form-data")
	public ResponseEntity<?> postProduct (@RequestPart(name = "producto",required = true) final Producto producto,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/products", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/products", usuario.getUSRToken());
			
			this.security.validateImg(img, null, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			this.service.manageProduct(producto, null,img, CPConstants.POST, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/products",produces = "application/json")
	public ResponseEntity<?> getProducts (HttpServletRequest request, Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/products", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/products", usuario.getUSRToken());
			
			List<ResponseProducto> productos = this.service.manageProduct(null, null, null,CPConstants.GET, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(200).body(productos);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/products/{productId}",consumes = "multipart/form-data")
	public ResponseEntity<?> patchProduct (@PathVariable(value = "productId",required = true) final String uuid,
										  @RequestPart(name = "producto",required = true) final Producto producto,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/products", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/products", usuario.getUSRToken());
			
			this.service.manageProduct(producto, uuid, img,CPConstants.PATCH, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/products/{productId}")
	public ResponseEntity<?> deleteProduct (@PathVariable(value = "productId",required = true) final String uuid,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/products", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/products", usuario.getUSRToken());
			
			this.service.manageProduct(null, uuid, null,CPConstants.DELETE, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/series/{productId}",consumes = "multipart/form-data")
	public ResponseEntity<?> postSerie (@PathVariable(value = "productId",required = true) final String uuidProduct,
										  @RequestPart(name = "serie",required = true) final Serie serie,
										  @RequestPart(name = "img",required = true)final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/series", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/series", usuario.getUSRToken());
			
			this.security.validateImg(img, "/series", usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			this.service.manageSerie(serie, null, img,uuidProduct, CPConstants.POST, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /series -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/series",produces = "application/json")
	public ResponseEntity<?> getSerie (
			@RequestParam(value="uuid",required = false) final String uuid,
			HttpServletRequest request, Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/series", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/series", usuario.getUSRToken());
			
			List<ResponseSerie> series = this.service.manageSerie(null, null, null, uuid,CPConstants.GET, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(200).body(series);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /series -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/series/{serieId}/product/{productId}",consumes = "multipart/form-data")
	public ResponseEntity<?> patchProduct (@PathVariable(value = "serieId",required = true) final String uuid,
										  @PathVariable(value = "productId",required = true) final String productUuid,
										  @RequestPart(name = "serie",required = true) final Serie serie,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/series", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/series", usuario.getUSRToken());
			
			this.service.manageSerie(serie, uuid, img,productUuid, CPConstants.PATCH, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /series -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/series/{serieId}")
	public ResponseEntity<?> deleteSerie (@PathVariable(value = "serieId",required = true) final String uuid,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/series", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/series", usuario.getUSRToken());
			
			this.service.manageSerie(null, uuid, null, null,CPConstants.DELETE, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /series -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/acabados",consumes = "multipart/form-data")
	public ResponseEntity<?> postAcabado (@RequestPart(name = "acabado",required = true) final ResponseAcabado acabado,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/adabados", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/acabados", usuario.getUSRToken());
			
			this.service.manageAcabado(acabado, null, img,CPConstants.POST, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /acabados -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/acabados",produces = "application/json")
	public ResponseEntity<?> getAcabados (@RequestParam(value="uuid",required = false) final String uuid,
			HttpServletRequest request, Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/acabados", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/acabados", usuario.getUSRToken());
			
			List<ResponseAcabado> acabados = this.service.manageAcabado(null, uuid, null,CPConstants.GET, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(200).body(acabados);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /acabados -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/acabados/{acabadoId}",consumes = "multipart/form-data")
	public ResponseEntity<?> patchProduct (@PathVariable(value = "acabadoId",required = true) final String uuid,
										  @RequestPart(name = "acabado",required = true) final ResponseAcabado acabado,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/acabados", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/acabados", usuario.getUSRToken());
			
			this.service.manageAcabado(acabado, uuid, img,CPConstants.PATCH, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /acabados -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/acabados/{acabadoId}")
	public ResponseEntity<?> deleteAcabado (@PathVariable(value = "acabadoId",required = true) final String uuid,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/acabados", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/acabados", usuario.getUSRToken());
			
			this.service.manageAcabado(null, uuid, null,CPConstants.DELETE, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /acabados -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/colores",consumes="multipart/form-data")
	public ResponseEntity<?> postColor (@RequestPart(name = "color",required = true)final ResponseColor body,
										@RequestPart(name = "img",required = true) final MultipartFile img,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/colores", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/colores", usuario.getUSRToken());
			
			this.service.manageColor(body, null, img,CPConstants.POST, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /acabados -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/colores",produces = "application/json")
	public ResponseEntity<?> getColores (@RequestParam(value="uuid",required = false) final String uuid,
			HttpServletRequest request, Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/colores", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/colores", usuario.getUSRToken());
			
			List<ResponseColor> colores = this.service.manageColor(null, uuid, null,CPConstants.GET, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(200).body(colores);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/colores/{colorId}",consumes = "multipart/form-data")
	public ResponseEntity<?> patchProduct (@PathVariable(value = "colorId",required = true) final String uuid,
										  @RequestPart(name = "color",required = true) final ResponseColor color,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/colores", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/colores", usuario.getUSRToken());
			
			this.service.manageColor(color, uuid, img,CPConstants.PATCH, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/colores/{colorId}")
	public ResponseEntity<?> deleteColor (@PathVariable(value = "colorId",required = true) final String uuid,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/colores", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/colores", usuario.getUSRToken());
			
			this.service.manageColor(null, uuid, null,CPConstants.DELETE, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /products -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/frentes",consumes="multipart/form-data")
	public ResponseEntity<?> postFrente (@RequestPart(name = "frente",required = true)final ResponseFrente body,
										 @RequestPart(name = "img",required = true) final MultipartFile img,
			HttpServletRequest request,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/frentes", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/frentes", usuario.getUSRToken());
			
			this.service.manageFrente(body, null, img,CPConstants.POST, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /frentes -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/frentes",produces = "application/json")
	public ResponseEntity<?> getFrentes (@RequestParam(value="uuid",required = false) final String uuid,
			HttpServletRequest request, Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/frentes", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.CLIENTE_ROLE, seguridad, "/frentes", usuario.getUSRToken());
			
			List<ResponseFrente> frentes = this.service.manageFrente(null, uuid, null,CPConstants.GET, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(200).body(frentes);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /frentes -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/frentes/{frenteId}",consumes = "multipart/form-data")
	public ResponseEntity<?> patchFrentes (@PathVariable(value = "frenteId",required = true) final String uuid,
										  @RequestPart(name = "frente",required = true) final ResponseFrente frente,
										  @RequestPart(name = "img",required = true) final MultipartFile img,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/frentes", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/frentes", usuario.getUSRToken());
			
			this.service.manageFrente(frente, uuid, img,CPConstants.PATCH, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /frentes -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/frentes/{frenteId}")
	public ResponseEntity<?> deleteFrente (@PathVariable(value = "frenteId",required = true) final String uuid,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/frentes", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/frentes", usuario.getUSRToken());
			
			this.service.manageFrente(null, uuid, null,CPConstants.DELETE, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /frentes -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/configuracion",consumes = "application/json")
	public ResponseEntity<?> postConfiguracion (@RequestBody(required = true) final ResponseConfiguracion config,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configuracion", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/configuracion", usuario.getUSRToken());
			
			this.configService.manageConfiguracion(config, null, null,CPConstants.POST, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /configuracion -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/configuracion",produces = "application/json")
	public ResponseEntity<?> getConfiguracion (@RequestParam(value = "uuid",required = false) final String uuid,
			HttpServletRequest request, Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configuracion", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/configuracion", usuario.getUSRToken());
			
			List<ResponseConfiguracion> config = this.configService.manageConfiguracion(null, null,uuid, CPConstants.GET, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			return ResponseEntity.status(200).body(config);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /configuracion -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.PATCH,value = "/configuracion/{configuracionId}",consumes = "application/json")
	public ResponseEntity<?> patchConfiguracion (@PathVariable(value = "configuracionId",required = true) final String referencia,
										  @RequestBody(required = true) final ResponseConfiguracion config,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configuracion", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/configuracion", usuario.getUSRToken());
			
			this.configService.manageConfiguracion(config, referencia, null,CPConstants.PATCH, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /configuracion -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
	
	@RequestMapping(method = RequestMethod.DELETE,value = "/configuracion/{configuracionId}")
	public ResponseEntity<?> deleteConfiguracion (@PathVariable(value = "configuracionId",required = true) final String referencia,
										  HttpServletRequest request,
										  Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/configuracion", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/configuracion", usuario.getUSRToken());
			
			this.configService.manageConfiguracion(null, referencia, null,CPConstants.DELETE, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /configuracion -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}
 	
	@RequestMapping(method = RequestMethod.POST,value = "/load")
	public ResponseEntity<?> loadData(HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/load", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/load", usuario.getUSRToken());
			
			this.service.launchData(usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
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
			
			log.error("[ERROR] -- /load -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/export")
	public ResponseEntity<?>export(@RequestParam(value="uuid",required = true) String uuid,
	HttpServletRequest request,Authentication authentication)
	{
		try 
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/export", seguridad);
			
			this.security.hierarchy(rolRepo, usuario.getRol(), CPConstants.ADMIN_ROLE, seguridad, "/export", usuario.getUSRToken());
		
			String response = this.configService.export(uuid, usuario.getRol().getNombre(), seguridad, usuario.getUSRToken());
			
			byte[] csvBytes = response.getBytes(StandardCharsets.UTF_8);

		    HttpHeaders headers = new HttpHeaders();
		    headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
		    headers.setContentDisposition(
		        ContentDisposition.attachment().filename("export.csv").build()
		    );
		    headers.setContentLength(csvBytes.length);
			
			return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /load -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
		
	}

}

