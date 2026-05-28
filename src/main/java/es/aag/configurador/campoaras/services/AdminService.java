package es.aag.configurador.campoaras.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
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
		
		log.info("[ACCION] -- /export-data -- {} Ha solicitado una exportación de los productos de la base de datos con permiso de {} -- {}",usrToken,rol,seguridad);
		
		return response;
	}
}
