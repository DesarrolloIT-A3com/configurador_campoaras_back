package es.aag.configurador.campoaras.services;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.aag.configurador.campoaras.dto.UserGetDTO;
import es.aag.configurador.campoaras.entities.Acabado;
import es.aag.configurador.campoaras.entities.AdminVerification;
import es.aag.configurador.campoaras.entities.Color;
import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.Frente;
import es.aag.configurador.campoaras.entities.Producto;
import es.aag.configurador.campoaras.entities.Serie;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IAcabadoRepository;
import es.aag.configurador.campoaras.repositories.IAdminVerificationRepository;
import es.aag.configurador.campoaras.repositories.IColorRepository;
import es.aag.configurador.campoaras.repositories.IConfiguracionRepository;
import es.aag.configurador.campoaras.repositories.IFrenteRepository;
import es.aag.configurador.campoaras.repositories.IProductoRepository;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.ISerieRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import es.aag.configurador.campoaras.utils.Validations;

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
	private IAdminVerificationRepository verRepo;
	
	@Autowired
	private MailService mail;
	
	@Autowired
	private EncryptorService encryptor;
	
	@Autowired
	private IProductoRepository productoRepo;
	
	@Autowired
	private ISerieRepository serieRepo;
	
	@Autowired
	private IConfiguracionRepository configRepo;
	
	@Autowired
	private IAcabadoRepository acabadoRepo;
	
	@Autowired
	private IColorRepository colorRepo;
	
	@Autowired
	private IFrenteRepository frenteRepo;
	
	private final Validations validation;
	
	@Autowired
	private PasswordEncoder encoder;

	public AdminService()
	{
		this.validation = new Validations();
	}
	
	/**
	 * Metodo que devuelve los usuarios registrados en la app, dependiendo del rol se devolverán determinados usuarios
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 */
	public List<UserGetDTO> getUsers(Usuario usuario,String rol,String seguridad,String usrToken) throws CPException
	{
		List<Usuario> usuarios = this.userRepo.findAll();
		List<UserGetDTO> response = new LinkedList<UserGetDTO>();
		
		if(!rol.equals(CPConstants.SUPADMIN_ROLE) &&  !rol.equals(CPConstants.ADMIN_ROLE) && !rol.equals(CPConstants.COMERCIAL_ROLE))
		{
			log.warn("[AVISO] -- /get-users -- {} Ha intentado obtener informacion de los usuarios con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		for(Usuario user:usuarios)
		{
			UserGetDTO dto = new UserGetDTO(user.getUuid(),this.encryptor.decrypt(user.getEmail()),this.encryptor.decrypt(user.getUsername()),user.getDescuento(),user.getSegundoDescuento(),this.encryptor.decrypt(user.getComercial()),user.getAcceso(),user.getRol().getNombre(),!user.getRol().getNombre().equals(CPConstants.VER_ROLE));
			
			String comercialEmail = this.encryptor.decrypt(usuario.getEmail());
			String userEmail = "";
			if(user.getComercial()!=null)
			{
				if(!user.getComercial().isBlank())
				{
					userEmail = this.encryptor.decrypt(user.getComercial());
				}
			}
			
			if(usuario.getRol().getNombre().equals(CPConstants.COMERCIAL_ROLE) && comercialEmail.equals(userEmail))
			{
				response.add(dto);
			}
			else if(!user.getRol().getNombre().equals(CPConstants.SUPADMIN_ROLE) && !usuario.getRol().getNombre().equals(CPConstants.COMERCIAL_ROLE))
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
			throw new CPException(400,"Datos invalidos");
		}
		
		if(body.getUsername().isBlank() || body.getDescuento()<0 || body.getSegundoDescuento()<0 || body.getRol().isBlank())
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha introducido datos invalidos para actualizar al usuario {} con permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
			throw new CPException(400,"Datos invalidos");
		}
		
		String comercial = this.encryptor.encrypt(body.getComercial());
		if(body.getComercial().isBlank() || body.getComercial().equalsIgnoreCase("ninguno"))
		{
			comercial = null;
		}
		
		usuario.setUsername(this.encryptor.encrypt(body.getUsername()));
		usuario.setComercial(comercial);
		usuario.setDescuento(body.getDescuento());
		usuario.setSegundoDescuento(body.getSegundoDescuento());
		usuario.setRol(this.rolRepo.findByNombre(body.getRol()));
		
		if(usuario.getRol().getNombre().equals(CPConstants.VER_ROLE))
		{
			usuario.setVerificado(false);
		}
		
		log.info("[ADMIN] -- /upt-user -- {} Ha actualizado al usuario {} con permiso de {} -- {}",usrToken,usuario.getUSRToken(),rol,seguridad);
		
		this.userRepo.save(usuario);		
	}
	
	public void sendCode(Map<String,String> body,Usuario usuario,String seguridad) throws CPException
	{
		String email = this.encryptor.decrypt(usuario.getEmail());
		String username = this.encryptor.decrypt(usuario.getUsername());
		
		String password = body.get("password");
		
		if(password==null)
		{
			log.warn("[AVISO] -- /verificate-action -- {} Ha intentado realizar un accion de {} introduciendo una contraseña nula con permiso de {} -- {}",usuario.getUSRToken(),CPConstants.SUPADMIN_ROLE,usuario.getRol().getNombre(),seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		if(!this.encoder.matches(password, usuario.getPassword()))
		{
			log.warn("[AVISO] -- /verificate-action -- {} Ha intentado realizar un accion de {} introduciendo una contraseña incorrecta con permiso de {} -- {}",usuario.getUSRToken(),CPConstants.SUPADMIN_ROLE,usuario.getRol().getNombre(),seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		AdminVerification verification = new AdminVerification();
		
		// Generacion de código de verificacion
		byte[] salt = new byte[8]; // CODIGO DE 8 BYTES
		new SecureRandom().nextBytes(salt);
		String saltBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
		
		// Generacion de fecga de caducidad 
		LocalDateTime endCode = LocalDateTime.now();
		endCode = endCode.plusMinutes(5);
		
		String uuid = UUID.randomUUID().toString();
		
		verification.setUuid(uuid);
		verification.setAdminUuid(usuario.getUuid());
		verification.setVerCode(this.encoder.encode(saltBase64));
		verification.setEndVerCode(endCode);
		
		this.verRepo.save(verification);
		this.verRepo.flush();
		this.mail.sendMailAdminVerification(username, email, usuario.getUSRToken(), seguridad, saltBase64);
		
		log.info("[ADMIN] -- /verificate-action -- {} Ha solicitado una accion de {} con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),CPConstants.SUPADMIN_ROLE,seguridad);
	
	}
	
	public List<Map<String,Object>> exportData(String uuid,String verCode,String rol,String seguridad,String usrToken) throws CPException
	{
		List<AdminVerification> verificaciones = this.verRepo.findByAdminUuid(uuid);
		
		validation.initialize(null, null, this.acabadoRepo, null, null, null, this.encryptor);
		
		AdminVerification found = null;
		
		for(AdminVerification item:verificaciones)
		{
			LocalDateTime now = LocalDateTime.now();
			
			if(!now.isAfter(item.getEndVerCode()))
			{
				if(this.encoder.matches(verCode, item.getVerCode()))
				{
					found = item;
				}
			}
		}
		
		if(found == null)
		{
			log.warn("[AVISO] -- /export-data -- {} Ha introducido un código de verificacion erroneo para exportar la base de datos con permiso de {} -- {}");
			throw new CPException(403,"No tienes permiso");
		}
		
		this.verRepo.deleteAll(verificaciones);
		this.verRepo.flush();
		
		List<Map<String,Object>> response = new LinkedList<Map<String,Object>>();
		
		// Extracción de acabados
		for(Acabado acabado:this.acabadoRepo.findAll())
		{
			Map<String,Object> item = new HashMap<String, Object>();
			item.put("entidad", "acabado");
			item.put("uuid", acabado.getUuid());
			item.put("nombre", this.encryptor.decrypt(acabado.getNombre()));
			
			String[] tipos = acabado.getTipos();
			
			for(int i = 0;i<tipos.length;i++)
			{
				tipos[i] = this.encryptor.decrypt(tipos[i]);
			}
			
			item.put("tipos", tipos);
			response.add(item);
		}
		
		//Extraccion de colores
		for(Color color:this.colorRepo.findAll())
		{
			Map<String,Object> item = new HashMap<String, Object>();
			item.put("entidad", "color");
			item.put("uuid", color.getUuid());
			item.put("nombre", this.encryptor.decrypt(color.getNombre()));
			
			List<String> acabados = new LinkedList<String>();
			for(Acabado acabado:color.getAcabados())
			{
				acabados.add(acabado.getUuid());
			}
			
			item.put("acabados", acabados);
			response.add(item);
		}
		
		// Extraccion de productos
		for(Producto producto:this.productoRepo.findAll())
		{
			Map<String,Object> item = new HashMap<String, Object>();
			item.put("entidad", "producto");
			item.put("uuid", producto.getUuid());
			item.put("nombre", this.encryptor.decrypt(producto.getNombre()));
			item.put("tipo", this.encryptor.decrypt(producto.getTipo()));
			item.put("cajon", producto.getCajon()!=null ? this.encryptor.decrypt(producto.getCajon()) : null);
			item.put("orden", producto.getOrden());
			response.add(item);
		}
		
		// Extraccion de variantes
		for(Serie serie:this.serieRepo.findAll())
		{
			Map<String,Object> item = new HashMap<String, Object>();
			item.put("entidad", "variante");
			item.put("uuid", serie.getUuid());
			item.put("variante", this.encryptor.decrypt(serie.getVariante()));
			item.put("modulo", this.encryptor.decrypt(serie.getModulo()));
			item.put("extra", this.encryptor.decrypt(serie.getExtra()));
			item.put("orden", serie.getOrden()!=null ? serie.getOrden() : 0);
			item.put("producto", serie.getProducto().getUuid());
			response.add(item);
		}
		
		// Extraccion de frentes
		for(Frente frente:this.frenteRepo.findAll())
		{
			Map<String,Object> item = new HashMap<String, Object>();
			item.put("entidad", "frente");
			item.put("uuid", frente.getUuid());
			item.put("nombre", this.encryptor.decrypt(frente.getNombre()));
			item.put("referencia", this.encryptor.decrypt(frente.getReferencia()));
			item.put("regleta", frente.isRegleta());
			item.put("tirador", frente.isTirador());
			
			List<String> acabados = new LinkedList<String>();
			List<String> acabadosExtension = new LinkedList<String>();
			List<String> productos = new LinkedList<String>();

			
			for(Acabado acabado:frente.getAcabados())
			{
				acabados.add(acabado.getUuid());
			}
			
			for(Acabado acabado:frente.getAcabadosExtension())
			{
				acabadosExtension.add(acabado.getUuid());
			}
						
			for(Producto producto:frente.getProductoFrente())
			{
				productos.add(producto.getUuid());
			}
			
			item.put("acabados", acabados);
			item.put("acabadosExtension", acabadosExtension);
			item.put("productos", productos);
			
			response.add(item);
		}
		
		// Extraccion de configuraciones
		for(Configuracion config:this.configRepo.findAll())
		{
			Map<String,Object> item = new HashMap<String, Object>();
			item.put("entidad", "configuracion");
			item.put("referencia", config.getReferencia());
			item.put("fondo", config.getFondo());
		    item.put("ancho", config.getAncho());
		    item.put("alto", config.getAlto());
		    item.put("altoMax", config.getAltoMax());
		    item.put("fondoMin", config.getFondoMin());
		    item.put("fondoMax", config.getFondoMax());
		    item.put("precioMedidaFondoEsp", config.getPrecioMedidaFondoEsp());
		    item.put("precioMedidaAnchoEsp", config.getPrecioMedidaAnchoEsp());
		    item.put("precioMedidaAltoEsp", config.getPrecioMedidaAltoEsp());
		    item.put("serie", config.getSerie().getUuid());
		    
		    List<Map<String,Object>> armazones = new LinkedList<Map<String,Object>>();
		    
		    if(config.getArmazon()!=null)
		    {
		    	for(Map<String,Object> armazon:config.getArmazon())
			    {
			    	Map<String,Object> itemArmazon = new HashMap<String, Object>();
			    	
			    	String nombre = this.encryptor.decrypt((String) armazon.get("nombre"));
			    	Acabado acabado = this.validation.findAcabado(nombre);
			    	
			    	if(acabado!=null)
			    	{
			    		itemArmazon.put("acabado", acabado.getUuid());
			    		Number rawPrecio = (Number) armazon.get("precio");
			    		itemArmazon.put("precio", rawPrecio.floatValue());
			    		armazones.add(itemArmazon);
			    	}
			    }
		    }
		  
		    item.put("armazon",armazones);
		    
		    List<Map<String,Object>> extras = new LinkedList<Map<String,Object>>();
		    
		    if(config.getExtras()!=null)
		    {
		    	for(Map<String,Object> extra:config.getExtras())
			    {
			    	Map<String,Object> itemExtra = new HashMap<String, Object>();
			    	
			    	String nombre = this.encryptor.decrypt((String) extra.get("nombre"));
		    		itemExtra.put("extra", nombre);
		    		Number rawPrecio = (Number) extra.get("precio");
		    		itemExtra.put("precio", rawPrecio.floatValue());
		    		extras.add(itemExtra);
			    }
		    }	
		    
		    
		    
		    item.put("extras",extras);

		    response.add(item);			
		}
		
		validation.destroy();
		
		log.info("[ADMIN] -- /export-data -- {} Ha solicitado una exportación de los productos de la base de datos con permiso de {} -- {}",usrToken,rol,seguridad);
		
		return response;
	}
	
	
	public void importData(MultipartFile json,String uuid,String verCode,String rol,String seguridad,String usrToken) throws CPException
	{
		List<AdminVerification> verificaciones = this.verRepo.findByAdminUuid(uuid);
		
		validation.initialize(null, null, this.acabadoRepo, null, null, null, this.encryptor);
		
		AdminVerification found = null;
		
		for(AdminVerification item:verificaciones)
		{
			LocalDateTime now = LocalDateTime.now();
			
			if(!now.isAfter(item.getEndVerCode()))
			{
				if(this.encoder.matches(verCode, item.getVerCode()))
				{
					found = item;
				}
			}
		}
		
		if(found == null)
		{
			log.warn("[AVISO] -- /export-data -- {} Ha introducido un código de verificacion erroneo para exportar la base de datos con permiso de {} -- {}");
			throw new CPException(403,"No tienes permiso");
		}
		
		this.verRepo.deleteAll(verificaciones);
		this.verRepo.flush();
	
		List<Map<String, Object>> jsonList = null;
		
		ObjectMapper objectMapper = new ObjectMapper();
		try
		{
		    byte[] content = json.getInputStream().readAllBytes();

		    jsonList = objectMapper.readValue(content, new TypeReference<List<Map<String, Object>>>() {});
		}
		catch (JsonParseException ex)
		{
		    log.error("[ERROR] -- /import-data -- {} Ha saltado un error JsonParseException al parsear el contenido del fichero JSON -- {}", usrToken, seguridad);
		    throw new CPException(400, "Datos invalidos");
		}
		catch (JsonMappingException ex)
		{
		    log.error("[ERROR] -- /import-data -- {} Ha saltado un error JsonMappingException al mapear el contenido del fichero JSON a List<Map<String,Object>> -- {}", usrToken, seguridad);
		    throw new CPException(400, "Datos invalidos");
		}
		catch (IOException ex)
		{
		    log.error("[ERROR] -- /import-data -- {} Ha saltado un error IOException al leer los bytes del fichero -- {}", usrToken, seguridad);
		    throw new CPException(500, "Error interno");
		}
	    
	    if(jsonList==null)
	    {
	    	log.warn("[AVISO] -- /import-data -- {} Ha introducido un json inválido resultando en un objeto nulo con permiso de {} -- {}",usrToken,rol,seguridad);
	    	throw new CPException(400,"Datos inválidos");
	    }
	    
	    // IMPORTACION DE ACABADOS
	    
	    List<Acabado> acabados = new LinkedList<Acabado>();
	    
	    for(Map<String,Object> item:jsonList)
	    {
	    	if(item.get("entidad").equals("acabado"))
	    	{
	    		String uuidItem = (String) item.get("uuid");
	    		String nombre = (String) item.get("nombre");
	    		String tipos[] = objectMapper.convertValue(item.get("tipos"), String[].class);	    		
	    		nombre = this.encryptor.encrypt(nombre);
	    		
	    		for(int i = 0;i<tipos.length;i++)
	    		{
	    			tipos[i] = this.encryptor.encrypt(tipos[i]);
	    		}
	    		
	    		Acabado acabado = new Acabado();
	    		acabado.setUuid(uuidItem);
	    		acabado.setNombre(nombre);
	    		acabado.setTipos(tipos);
	    		acabados.add(acabado);
	    	}
	    	
	    	
	    }
	    this.acabadoRepo.saveAll(acabados);
	    this.acabadoRepo.flush();
	    
	    // IMPORTACION DE COLORES
	    
	    List<Color> colores = new LinkedList<Color>();
	    
	    for(Map<String,Object> item:jsonList)
	    {
	    	if(item.get("entidad").equals("color"))
	    	{
	    		String uuidItem = (String) item.get("uuid");
	    		String nombre = (String) item.get("nombre");
	    		
	    		String[] acabadosItem = objectMapper.convertValue(item.get("acabados"), String[].class);
	    		
	    		Color color = new Color();
	    		color.setUuid(uuidItem);
	    		color.setNombre(this.encryptor.encrypt(nombre));
	    		colores.add(color);
	    		
	    		for(String uuidAcabado:acabadosItem)
	    		{
	    			for(Acabado acabado:acabados)
	    			{
	    				if(acabado.getUuid().equals(uuidAcabado))
	    				{
	    					color.addAcabado(acabado);
	    					break;
	    				}
	    			}
	    		}
	    		
	    		colores.add(color);
	    	}
	    }
	    
	    this.colorRepo.saveAll(colores);
	    this.colorRepo.flush();
	    
	    this.acabadoRepo.saveAll(acabados);
	    this.acabadoRepo.flush();	    
	    
	    List<Producto> productos = new LinkedList<Producto>();
	    
	    // IMPORTACION DE PRODUCTOS
	    for(Map<String,Object> item:jsonList)
	    {
	        if(item.get("entidad").equals("producto"))
	        {
	            String uuidItem = (String) item.get("uuid");
	            String nombre = (String) item.get("nombre");
	            String tipo = (String) item.get("tipo");
	            String cajon = (String) item.get("cajon");
	    		Integer orden = (Integer) item.get("orden");

	            
	            // Encriptar los campos de texto
	            nombre = this.encryptor.encrypt(nombre);
	            tipo = this.encryptor.encrypt(tipo);
	            
	            if(cajon != null && !cajon.isEmpty())
	            {
	                cajon = this.encryptor.encrypt(cajon);
	            }
	            
	            // Crear y configurar la entidad Producto
	            Producto producto = new Producto();
	            producto.setUuid(uuidItem);
	            producto.setNombre(nombre);
	            producto.setTipo(tipo);
	            producto.setCajon(cajon); // Puede ser null
	            producto.setOrden(orden!=null ? orden : 0);
	            
	            productos.add(producto);
	        }
	    }
	    
	    this.productoRepo.saveAll(productos);
	    this.productoRepo.flush();
	    
	    // IMPORTACION DE VARIANTES (SERIES)
	    
	    List<Serie> series = new LinkedList<Serie>();

	    for(Map<String,Object> item:jsonList)
	    {
	        if(item.get("entidad").equals("variante"))
	        {
	            String uuidItem = (String) item.get("uuid");
	            String variante = (String) item.get("variante");
	            String modulo = (String) item.get("modulo");
	            String extra = (String) item.get("extra");
	            Integer orden = (Integer) item.get("orden");
	            String uuidProducto = (String) item.get("producto");
	            
	            // Encriptar los campos de texto
	            variante = this.encryptor.encrypt(variante);
	            modulo = this.encryptor.encrypt(modulo);
	            
	            if(extra != null && !extra.isEmpty())
	            {
	                extra = this.encryptor.encrypt(extra);
	            }
	            
	            // Crear la entidad Serie
	            Serie serie = new Serie();
	            serie.setUuid(uuidItem);
	            serie.setVariante(variante);
	            serie.setModulo(modulo);
	            if(extra==null)
	            {
	            	serie.setExtra(null);
	            }
	            else
	            {
	            	serie.setExtra(extra.isEmpty() ? null : extra);
	            }
	            serie.setOrden(orden!=null ? orden : 0);
	            
	            // Buscar y relacionar el producto
	            for(Producto producto : productos)
	            {
	                if(producto.getUuid().equals(uuidProducto))
	                {
	                    serie.setProducto(producto);
	                    break;
	                }
	            }
	            
	            series.add(serie);
	        }
	    }

	    // Guardar primero las series y luego los productos (o al revés según dependencias)
	    this.serieRepo.saveAll(series);
	    this.serieRepo.flush();

	    this.productoRepo.saveAll(productos);
	    this.productoRepo.flush();
	    
	 // IMPORTACION DE FRENTES

	    List<Frente> frentes = new LinkedList<Frente>();

	    // Crear mapas para búsqueda rápida por UUID
	    Map<String, Acabado> acabadoMap = new HashMap<>();
	    for(Acabado acabado : acabados)
	    {
	        acabadoMap.put(acabado.getUuid(), acabado);
	    }

	    Map<String, Producto> productoMap = new HashMap<>();
	    for(Producto producto : productos)
	    {
	        productoMap.put(producto.getUuid(), producto);
	    }

	    for(Map<String,Object> item:jsonList)
	    {
	        if(item.get("entidad").equals("frente"))
	        {
	            String uuidItem = (String) item.get("uuid");
	            String nombre = (String) item.get("nombre");
	            String referencia = (String) item.get("referencia");
	            Boolean regleta = (Boolean) item.get("regleta");
	            Boolean tirador = (Boolean) item.get("tirador");
	            
	            // Listas de UUIDs que vienen en el JSON
	            String[] acabadosUuids = objectMapper.convertValue(item.get("acabados"), String[].class);;
	            String[] acabadosExtensionUuids = objectMapper.convertValue(item.get("acabadosExtension"), String[].class);;
	            String[] productosUuids = objectMapper.convertValue(item.get("productos"), String[].class);;
	            
	            // Encriptar campos de texto
	            nombre = this.encryptor.encrypt(nombre);
	            referencia = this.encryptor.encrypt(referencia);
	            
	            // Crear la entidad Frente
	            Frente frente = new Frente();
	            frente.setUuid(uuidItem);
	            frente.setNombre(nombre);
	            frente.setReferencia(referencia);
	            frente.setRegleta(regleta != null ? regleta : false);
	            frente.setTirador(tirador != null ? tirador : false);
	            
	            // Relacionar acabados
	            if(acabadosUuids != null)
	            {
	                for(String uuidAcabado : acabadosUuids)
	                {
	                    Acabado acabado = acabadoMap.get(uuidAcabado);
	                    if(acabado != null)
	                    {
	                        frente.addAcabado(acabado);
	                    }
	                }
	            }
	            
	            // Relacionar acabados de extension
	            if(acabadosExtensionUuids != null)
	            {
	                for(String uuidAcabadoExt : acabadosExtensionUuids)
	                {
	                    Acabado acabadoExt = acabadoMap.get(uuidAcabadoExt);
	                    if(acabadoExt != null)
	                    {
	                        frente.addAcabadoExtension(acabadoExt);
	                    }
	                }
	            }
	            
	            // Relacionar productos
	            if(productosUuids != null)
	            {
	                for(String uuidProducto : productosUuids)
	                {
	                    Producto producto = productoMap.get(uuidProducto);
	                    if(producto != null)
	                    {
	                        frente.addProducto(producto);
	                    }
	                }
	            }
	            
	            frentes.add(frente);
	        }
	    }

	    // Guardar los frentes
	    this.frenteRepo.saveAll(frentes);
	    this.frenteRepo.flush();
	    
	    this.acabadoRepo.saveAll(acabados);
	    this.acabadoRepo.flush();
	    
	    this.productoRepo.saveAll(productos);
	    this.productoRepo.flush();
	    
	    
	 // IMPORTACION DE CONFIGURACIONES

	    List<Configuracion> configuraciones = new LinkedList<Configuracion>();

	    // Crear mapa de series para búsqueda rápida por UUID
	    Map<String, Serie> serieMap = new HashMap<>();
	    for(Serie serie : series)
	    {
	        serieMap.put(serie.getUuid(), serie);
	    }

	    // Crear mapa de acabados por UUID
	    acabadoMap = new HashMap<>();
	    for(Acabado acabado : acabados)
	    {
	        acabadoMap.put(acabado.getUuid(), acabado);
	    }

	    for(Map<String,Object> item:jsonList)
	    {
	        if(item.get("entidad").equals("configuracion"))
	        {
	            Configuracion configuracion = new Configuracion();
	            
	            // Campos simples (con null safety)
	            configuracion.setReferencia((String) item.get("referencia"));
	            
	            // Campos numéricos (pueden ser Integer, Long, etc.)
	            configuracion.setFondo(item.get("fondo") != null ? ((Number) item.get("fondo")).floatValue() : null);
	            configuracion.setAncho(item.get("ancho") != null ? ((Number) item.get("ancho")).floatValue() : null);
	            configuracion.setAlto(item.get("alto") != null ? ((Number) item.get("alto")).floatValue() : null);
	            configuracion.setAltoMax(item.get("altoMax") != null ? ((Number) item.get("altoMax")).floatValue() : null);
	            configuracion.setFondoMin(item.get("fondoMin") != null ? ((Number) item.get("fondoMin")).floatValue() : null);
	            configuracion.setFondoMax(item.get("fondoMax") != null ? ((Number) item.get("fondoMax")).floatValue() : null);
	            
	            // Campos Float
	            configuracion.setPrecioMedidaFondoEsp(item.get("precioMedidaFondoEsp") != null ? 
	                ((Number) item.get("precioMedidaFondoEsp")).floatValue() : null);
	            configuracion.setPrecioMedidaAnchoEsp(item.get("precioMedidaAnchoEsp") != null ? 
	                ((Number) item.get("precioMedidaAnchoEsp")).floatValue() : null);
	            configuracion.setPrecioMedidaAltoEsp(item.get("precioMedidaAltoEsp") != null ? 
	                ((Number) item.get("precioMedidaAltoEsp")).floatValue() : null);
	            
	            // Relacionar serie
	            String serieUuid = (String) item.get("serie");
	            if(serieUuid != null)
	            {
	                configuracion.setSerie(serieMap.get(serieUuid));
	            }
	            
	            // Procesar armazones (List<Map<String,Object>>)
	            List<Map<String,Object>> armazonesList = objectMapper.convertValue(
	            	    item.get("armazon"),
	            	    new TypeReference<List<Map<String, Object>>>() {}
	            	);
	            if(armazonesList != null && !armazonesList.isEmpty())
	            {
	                List<Map<String,Object>> armazonesProcesados = new ArrayList<>();
	                
	                for(Map<String,Object> armazon : armazonesList)
	                {
	                    Map<String,Object> armazonProcesado = new HashMap<>();
	                    
	                    // Obtener el acabado por UUID
	                    String acabadoUuid = (String) armazon.get("acabado");
	                    Acabado acabado = acabadoMap.get(acabadoUuid);
	                    
	                    if(acabado != null)
	                    {
	                        armazonProcesado.put("nombre", acabado.getNombre());
	                        
	                        // Obtener el precio (puede ser Integer, Double, Float)
	                        Number precioNumber = (Number) armazon.get("precio");
	                        if(precioNumber != null)
	                        {
	                            armazonProcesado.put("precio", precioNumber.floatValue());
	                            armazonesProcesados.add(armazonProcesado);
	                        }
	                    }
	                }
	                
	                configuracion.setArmazon(armazonesProcesados);
	            }
	            
	            // Procesar extras (List<Map<String,Object>>)
	            List<Map<String,Object>> extrasList = objectMapper.convertValue(
	            	    item.get("extras"),
	            	    new TypeReference<List<Map<String, Object>>>() {}
	            	);
	            if(extrasList != null && !extrasList.isEmpty())
	            {
	                List<Map<String,Object>> extrasProcesados = new ArrayList<>();
	                
	                for(Map<String,Object> extra : extrasList)
	                {
	                    Map<String,Object> extraProcesado = new HashMap<>();
	                    
	                    // El extra ya viene como texto desencriptado desde la exportación
	                    String extraNombre = (String) extra.get("extra");
	                    if(extraNombre != null)
	                    {
	                        extraProcesado.put("nombre", this.encryptor.encrypt(extraNombre));
	                        
	                        // Obtener el precio
	                        Number precioNumber = (Number) extra.get("precio");
	                        if(precioNumber != null)
	                        {
	                            extraProcesado.put("precio", precioNumber.floatValue());
	                            extrasProcesados.add(extraProcesado);
	                        }
	                    }
	                }
	                
	                configuracion.setExtras(extrasProcesados);
	            }
	            
	            configuraciones.add(configuracion);
	        }
	    }

	    // Guardar todas las configuraciones 
        this.configRepo.saveAll(configuraciones);
        this.configRepo.flush();
	    
	    
	    log.info("[ADMIN] -- /import-data -- {} Ha importado una base de datos en JSON de productos con permiso de {} -- {}",usrToken,rol,seguridad);
	}
}
