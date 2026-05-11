package es.aag.configurador.campoaras.services;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.aag.configurador.campoaras.configurations.JwtUtil;
import es.aag.configurador.campoaras.dto.AuthDTO;
import es.aag.configurador.campoaras.dto.LoginDTO;
import es.aag.configurador.campoaras.entities.Rol;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.entities.Verification;
import es.aag.configurador.campoaras.entities.VerificationPass;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IVerificationPassRepository;
import es.aag.configurador.campoaras.repositories.IVerificationRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;

/**
 * Servicio encargado de la autenticación de usuario
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@Service
public class AuthService 
{
	private Logger log = LogManager.getLogger();
	
	private final long ACC_EXP_TIME = 30 * 60 * 1000; // 30 minutos
	private final long REF_EXP_TIME = 7 * 24 * 60 * 60; // 7 dias
	
	@Autowired
	private IUsuarioRepository usuarioRepo;
	
	@Autowired
	private IRolRepository rolRepo;
	
	@Autowired
	private IVerificationRepository verRepo;
	
	@Autowired
	private IVerificationPassRepository verPassRepo;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	private EncryptorService encryptor;
	
	@Autowired
	private MailService mailService;
	
	
	private PasswordEncoder passwordEncoder;
	

	public AuthService(PasswordEncoder passwordEncoder)
	{
		this.passwordEncoder = passwordEncoder;
		
	}
	
	/**
	 * Metodo que registra a un usuario en la entidad Verification con un código expirable, ese código caduca en 10 minutos
	 * @param body
	 * @param seguridad
	 * @throws CPException
	 */
	public void verificate(Verification body,String seguridad) throws CPException
	{
		List<Usuario> usuarios = usuarioRepo.findAll();
			
		int index = 0;
		
		// Filtro para verificar que no se repite un usuario
		while(index<usuarios.size())
		{
			Usuario item = usuarios.get(index);
			
			if(encryptor.decrypt(item.getEmail()).equals(body.getEmail()) || encryptor.decrypt(item.getUsername()).equals(body.getUsername()))
			{	
				log.info("[AVISO] Se ha intentado acceder con datos de otro usuario - {} -- {}",item.getUSRToken(),seguridad);
				throw new CPException(409,"Los datos que rellenas ya existen");
			}
			
			index++;
		}
		
		List<Verification> verificaciones = this.verRepo.findAll();
		index = 0;
		boolean found = false;
		
		// Operación para encontrar a un usuario que se haya querido verificar de nuevo
		while(index<verificaciones.size() && !found)
		{
			Verification item = verificaciones.get(index);
			
			if(encryptor.decrypt(item.getEmail()).equals(body.getEmail()) || encryptor.decrypt(item.getUsername()).equals(body.getUsername()))
			{
				body = item;
				found = true;
			}
			
			index++;
		}
		
		String username = "";
		String mail = "";
		String token = "";
			
		if(!found)
		{
			username = body.getUsername();
			mail = body.getEmail();
			String uuid = UUID.randomUUID().toString();
			body.setUuid(uuid);
			body.setEmail(this.encryptor.encrypt(body.getEmail()));
			body.setUsername(this.encryptor.encrypt(body.getUsername()));
			body.setPassword(this.passwordEncoder.encode(body.getPassword()));
			token = "USR-" + UUID.randomUUID().toString().substring(0,8);
			body.setUSRtoken(token);
		}
		else
		{
			username = encryptor.decrypt(body.getUsername());
			mail = encryptor.decrypt(body.getEmail());
		}
        
		// Generacion de código de verificacion
		byte[] salt = new byte[8]; // CODIGO DE 8 BYTES
		new SecureRandom().nextBytes(salt);
		String saltBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
        
        // Generación de fecha de caducidad
        LocalDateTime endCode = LocalDateTime.now();
        endCode = endCode.plusMinutes(5);
        body.setEndVerCode(endCode);
		
        this.mailService.sendMail(username,mail,token,seguridad,saltBase64);
        
        body.setVerCode(this.passwordEncoder.encode(saltBase64));
        
        this.verRepo.save(body);
	}
	
	/**
	 * Metodo que valida el código de verificación y registra al usuario en su entidad correspondiente si el código ha pasado la validación
	 * @param code
	 * @param seguridad
	 * @throws CPException
	 */
	public void register(String code,String seguridad) throws CPException
	{
		Verification verify = null;
		
		List<Verification> verifyList = this.verRepo.findAll();
		
		int index = 0;
		
		while(index<verifyList.size() && verify==null)
		{
			verify = verifyList.get(index);
			
			if(!this.passwordEncoder.matches(code, verify.getVerCode()))
			{
				verify = null;
			}
				
			index++;
		}
		
		if(verify==null)
		{
			log.info("[AVISO] -- /verifify --  Se ha intentado acceder con un código inexistente -- {}",seguridad);
			throw new CPException(403,"El código introducido es inválido o ha expirado");
		}
		else
		{
			LocalDateTime now = LocalDateTime.now();
			if(now.isAfter(verify.getEndVerCode()))
			{
				log.info("[AVISO] -- /verifify -- Se ha intentado acceder con un código expirado, valor introdudcido: {} -- {}",code,seguridad);
				throw new CPException(400,"El código introducido es inválido o ha expirado");
			}
		}
		
		Usuario body = new Usuario();
		String uuid = UUID.randomUUID().toString();
		body.setUuid(uuid);
		body.setEmail(verify.getEmail());
		body.setUsername(verify.getUsername());
		body.setPassword(verify.getPassword());
		body.setAcceso(LocalDateTime.now());
		String token = "USR-" + UUID.randomUUID().toString().substring(0,8);
		body.setUSRToken(token);
		body.setRol(this.rolRepo.findByNombre(CPConstants.VER_ROLE));
		body.setVerificado(false);
		
		this.usuarioRepo.save(body);
		
		this.verRepo.delete(verify);
		
		log.info("[ACCION] -- /verifify -- {} se ha registrado -- {}",token,seguridad);
	}
	
	/**
	 * Metodo que permite a un usuario iniciar sesion
	 * @param body
	 * @param seguridad
	 * @return
	 * @throws CPException
	 */
	public AuthDTO login (LoginDTO body,String seguridad) throws CPException
	{
		CPException ex = new CPException(401,"Datos inválidos");
		Usuario usuario = this.findUserByEmail(body.getEmail());
		
		if(usuario == null)
		{
			log.warn("[AVISO] -- /login -- Intento de acceso con correo no válido -- {}",seguridad);
			throw ex;
		}
		
		if(!this.passwordEncoder.matches(body.getPassword(), usuario.getPassword()))
		{
			log.warn("[AVISO] -- /login -- Intento de acceso con credenciales no válidas -- {}",seguridad);
			throw ex;
		}
		
		String accessToken = jwtUtil.generateToken(usuario.getUuid());
		String refreshToken = jwtUtil.generateRefreshToken(usuario.getUuid());
		log.info("[ACCION] -- /login -- {} Ha accedido correctamente -- {}",usuario.getUSRToken(),seguridad);
		
		return new AuthDTO(accessToken, refreshToken, this.ACC_EXP_TIME, this.REF_EXP_TIME);
	}
	
	/**´
	 * Metodo que autentica un usuario por JWT
	 * @param authentication
	 * @param seguridad
	 * @return
	 * @throws CPException
	 */
	public Map<String,Object> authUser(Authentication authentication,String seguridad) throws CPException
	{
		if(authentication == null || !authentication.isAuthenticated())
		{
			log.warn("[AVISO] -- /me -- Acceso sin autenticación válida -- {}",seguridad);
			throw new CPException(401,"No autenticado");
		}
		
		Map<String,Object> response = new HashMap<String,Object>();
		String uuid = authentication.getName();
		Optional<Usuario> optUser = this.usuarioRepo.findById(uuid);
		
		if(!optUser.isPresent())
		{
			log.warn("[AVISO] -- /me -- Intento de acceso con un token erroneo o caducado -- {}",seguridad);
			throw new CPException(403,"Credenciales de sesion inválidas");
		}
		
		Usuario usuario = optUser.get();
		
		response.put("uuid", usuario.getUuid());
		response.put("email", this.encryptor.decrypt(usuario.getEmail()));
		response.put("username", this.encryptor.decrypt(usuario.getUsername()));
		
		log.info("[ACCION] -- /me -- {} ha accedido con un token valido -- {}",usuario.getUSRToken(),seguridad);
		
		usuario.setAcceso(LocalDateTime.now());
		
		this.usuarioRepo.save(usuario);
		
		return response;
	}
	
	/**
	 * Metodo que envía un código de verificación para recuperar la cuenta en caso de olvido de contraseña
	 * @param email
	 * @param seguridad
	 * @throws CPException
	 */
	public void verificateForgetPass(String email, String seguridad) throws CPException
	{
		Usuario usuario = this.findUserByEmail(email);
		
		if(usuario == null)
		{
			log.info("[AVISO] -- /forget-password --  Se ha intentado solicitar un código de cambio de contraseña con un usuario inexistente - {} -- {}",email,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		VerificationPass verification = this.verPassRepo.findByEmail(usuario.getEmail());
		
		if(verification == null)
		{
			verification = new VerificationPass();
			verification.setUuid(UUID.randomUUID().toString());
			verification.setEmail(usuario.getEmail());
			verification.setUSRToken(usuario.getUSRToken());
		}
		
		// Generacion de código de verificacion
		byte[] salt = new byte[8]; // CODIGO DE 8 BYTES
		new SecureRandom().nextBytes(salt);
		String saltBase64 = Base64.getUrlEncoder().withoutPadding().encodeToString(salt);
        
        // Generación de fecha de caducidad
        LocalDateTime endCode = LocalDateTime.now();
        endCode = endCode.plusMinutes(10);
        
        verification.setVerCode(this.passwordEncoder.encode(saltBase64));
        verification.setEndVerCode(endCode);
        
        log.info("[ACCION] -- /forget-pass -- {} Ha solicitado un código de verificacion para cambiar su contraseña -- {}",usuario.getUSRToken(),seguridad);
        
        this.mailService.sendMailForgetPass(this.encryptor.decrypt(usuario.getUsername()), this.encryptor.decrypt(usuario.getEmail()),usuario.getUSRToken(),seguridad,saltBase64);
                
        this.verPassRepo.save(verification);
	}
	
	/**
	 * Metodo que cambia la contraseña mediante una verificación
	 * @param verificate
	 * @param newPass
	 * @param seguridad
	 * @throws CPException
	 */
	public void changePass(String verificate,String newPass,String seguridad) throws CPException
	{
		List<VerificationPass> verification = this.verPassRepo.findAll();
		
		int index = 0;
		
		VerificationPass verify = null;
		
		while(index<verification.size() && verify==null)
		{
			verify = verification.get(index);
			
			if(!this.passwordEncoder.matches(verificate, verify.getVerCode()))
			{
				verify = null;
			}
			
			index++;
		}
		
		if(verify==null)
		{
			log.info("[AVISO] -- /reset-password -- Se ha introducido un código de verificacion inexistente para cambiar la contraseña -- {}",seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		if(newPass == null)
		{
			log.info("[AVISO] -- /reset-password -- {} Ha intentado acceder al cambio de contraseña con un valor nulo en el body -- {}",verify.getUSRToken(),seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		if(newPass.isEmpty())
		{
			log.info("[AVISO] -- /reset-password -- {} Ha intentado acceder al cambio de contraseña con un valor vacio en el body -- {}",verify.getUSRToken(),seguridad);
			throw new CPException(403,"Datos inválidos");
		}
		
		if(newPass.equals(CPConstants.MAP_DEFAULT_VALUE))
		{
			log.info("[AVISO] -- /reset-password -- {} Ha intentado acceder al cambio de contraseña con un valor nulo en el body -- {}",verify.getUSRToken(),seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		LocalDateTime date = LocalDateTime.now();
		
		if(date.isAfter(verify.getEndVerCode()))
		{
			log.info("[AVISO] -- /reset-password -- {} Ha introducido un código caducado para cambiar su contraseña -- {}",verify.getUSRToken(),seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		if(!this.passwordEncoder.matches(verificate, verify.getVerCode()))
		{
			log.info("[AVISO] -- /reset-password -- {} Ha introducido un codigo de verificacion erroneo para cambiar la contraseña -- {}",seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		Usuario usuario = this.usuarioRepo.findByEmail(verify.getEmail());
		
		if(this.passwordEncoder.matches(newPass, usuario.getPassword()))
		{
			log.info("[AVISO] -- /reset-password -- {} Ha intentado cambiar su contraseña usando la suya original -- {}",usuario.getUSRToken(),seguridad);
			throw new CPException(400,"Solicitud erronea");
		}
		
		usuario.setPassword(this.passwordEncoder.encode(newPass));
		
		this.verPassRepo.delete(verify);
		this.usuarioRepo.save(usuario);
		
		log.info("[ACCION] {} Ha cambiado su contraseña mediante recuperacion -- {}",usuario.getUSRToken(),seguridad);
	}
	
	/**
	 * Metodo que devuelve las capacidades de usuario segun el rol con el que tenga asociado
	 * @param rol
	 * @param seguridad
	 * @param endpoint
	 * @param userToken
	 * @return
	 * @throws CPException
	 */
	public Map<String,Boolean> getCapabilities(Rol rol,String seguridad,String endpoint,String userToken) throws CPException
	{
		
		if(userToken == null)
		{
			log.info("[AVISO] -- {} -- Se ha intentado acceder sin user-token -- {}",endpoint,seguridad);
			throw new CPException(403,"No tienes permisos de acceso");
		}
		
		String rolename = rol.getNombre();
		
		// Carga basica de capabilities
		Map<String,Boolean> capabilities = CPConstants.FILL_CAPABILITIES();
		
		switch(rolename)
		{
			
			case CPConstants.VER_ROLE:
			{
				log.info("[ACCION] -- {} -- {} Ha accedido con capacidades de {} -- {}",endpoint,userToken,rolename,seguridad);
				break;
			}
			case CPConstants.CLIENTE_ROLE:
			{
				log.info("[ACCION] -- {} -- {} Ha accedido con capacidades de {} -- {}",endpoint,userToken,rolename,seguridad);
				capabilities.put("general_access", true);
				break;
			}
			case CPConstants.COMERCIAL_ROLE:
			{
				log.info("[ACCION] -- {} -- {} Ha accedido con capacidades de {} -- {}",endpoint,userToken,rolename,seguridad);
				capabilities.put("general_access", true);
				capabilities.put("admin_panel",true);
				capabilities.put("users_read", true);
				break;
			}
			case CPConstants.ADMIN_ROLE:
			{
				log.info("[ACCION] -- {} -- {} Ha accedido con capacidades de {} -- {}",endpoint,userToken,rolename,seguridad);
				capabilities.put("general_access", true);
				capabilities.put("admin_panel",true);
				capabilities.put("users_read", true);
		    	capabilities.put("users_write", true);
		    	capabilities.put("managment",true);
				break;
			}
			case CPConstants.SUPADMIN_ROLE:
			{
				log.info("[ACCION] -- {} -- {} Ha accedido con capacidades de {} -- {}",endpoint,userToken,rolename,seguridad);
				capabilities.put("general_access", true);
		    	capabilities.put("admin_panel", true);
		    	capabilities.put("managment", true);
		    	capabilities.put("users_read", true);
		    	capabilities.put("users_write", true);
		    	capabilities.put("logs_read", true);
		    	capabilities.put("admin_read", true);
		    	capabilities.put("admin_write", true);
				break;
			}
			default:
			{
				log.info("[AVISO] -- {} -- {} Ha intentado acceder con un rol no válido {} -- {}",userToken,rolename,seguridad);
				throw new CPException(403,"No tienes permisos de acceso");
			}
			
		}
		
		return capabilities;
	}
	
	/**
	 * Método que busca un usuario por su email
	 * @param value
	 * @return Usuario encontrado o null si no lo encuentra
	 */
	private Usuario findUserByEmail(String value)
	{
		List<Usuario> usuarios = this.usuarioRepo.findAll();
		
		int index = 0;
		Usuario user = null;
		
		while(user == null && index<usuarios.size())
		{
			Usuario item = usuarios.get(index);
			
			if(this.encryptor.decrypt(item.getEmail()).equals(value))
			{
				user = item;
			}
			
			index++;
		}
		
		return user;
	}
}
