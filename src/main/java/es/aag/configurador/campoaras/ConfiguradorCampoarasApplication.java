package es.aag.configurador.campoaras;

import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.aag.configurador.campoaras.entities.Rol;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IProductoRepository;
import es.aag.configurador.campoaras.repositories.IRolRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.services.EncryptorService;
import es.aag.configurador.campoaras.utils.CPConstants;

@SpringBootApplication
@ComponentScan(basePackages = "es.aag.configurador.campoaras")
public class ConfiguradorCampoarasApplication implements CommandLineRunner{
	
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private IUsuarioRepository userCreator;
	
	@Autowired
	private IRolRepository rolCreator;
	
	@Autowired
	private EncryptorService encryptor;

	public static void main(String[] args) 
	{
		SpringApplication.run(ConfiguradorCampoarasApplication.class, args);
	}
	
	public void run(String...args)
	{
		log.info("[AVISO] Arranque de la app");
	
		PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
		
		if(rolCreator.count()==0)
		{
			Rol superAdmin = new Rol();
			Rol adminRol = new Rol();
			Rol comercialRol = new Rol();
			Rol clienteRol = new Rol();
			Rol verificateRol = new Rol();
			
			superAdmin.setUuid(UUID.randomUUID().toString());
			superAdmin.setNombre(CPConstants.SUPADMIN_ROLE);
			adminRol.setUuid(UUID.randomUUID().toString());
			adminRol.setNombre(CPConstants.ADMIN_ROLE);
			comercialRol.setUuid(UUID.randomUUID().toString());
			comercialRol.setNombre(CPConstants.COMERCIAL_ROLE);
			clienteRol.setUuid(UUID.randomUUID().toString());
			clienteRol.setNombre(CPConstants.CLIENTE_ROLE);
			verificateRol.setUuid(UUID.randomUUID().toString());
			verificateRol.setNombre(CPConstants.VER_ROLE);
			
			this.rolCreator.save(superAdmin);
			this.rolCreator.save(adminRol);
			this.rolCreator.save(clienteRol);
			this.rolCreator.save(comercialRol);
			this.rolCreator.save(verificateRol);
			log.info("[ADMIN] creación de roles base");
		}
		
		if(userCreator.count()==0)
		{
			Usuario superUser = new Usuario();
			UUID uuid = UUID.randomUUID();
			
			superUser.setUuid(uuid.toString());
			superUser.setEmail(encryptor.encrypt(CPConstants.ADMIN_MAIL));
			superUser.setUsername(encryptor.encrypt(CPConstants.ADMIN_NAME[0]));
//			superUser.setPassword(passwordEncoder.encode(CPConstants.ADMIN_PASS[0]));
			superUser.setPassword(passwordEncoder.encode("Prueba12345_"));
			superUser.setDescuento(0);
			superUser.setComercial(null);
			superUser.setUSRToken("USR-"+UUID.randomUUID().toString().substring(0,8));
			superUser.setRol(this.rolCreator.findByNombre(CPConstants.SUPADMIN_ROLE));
			
			this.userCreator.save(superUser);
			
			log.info("[ADMIN] Creacion de super usuario");
			
			Usuario testUser = new Usuario();
			uuid = UUID.randomUUID();
			
			testUser.setUuid(uuid.toString());
			testUser.setEmail(this.encryptor.encrypt("pruiz@a3com.es"));
			testUser.setPassword(passwordEncoder.encode("Prueba12345_"));
			testUser.setUsername(this.encryptor.encrypt("Pablo Ruiz"));
			testUser.setDescuento(0);
			testUser.setComercial(null);
			testUser.setUSRToken("USR-"+UUID.randomUUID().toString().substring(0,8));
			testUser.setRol(this.rolCreator.findByNombre(CPConstants.ADMIN_ROLE));
			
			this.userCreator.save(testUser);
			
			Usuario testUser2 = new Usuario();
			uuid = UUID.randomUUID();
			
			testUser2.setUuid(uuid.toString());
			testUser2.setEmail(this.encryptor.encrypt("prueba@a3com.es"));
			testUser2.setPassword(passwordEncoder.encode("Prueba12345_"));
			testUser2.setUsername(this.encryptor.encrypt("Usuario Prueba"));
			testUser2.setDescuento(0);
			testUser2.setComercial(null);
			testUser2.setUSRToken("USR-"+UUID.randomUUID().toString().substring(0,8));
			testUser2.setRol(this.rolCreator.findByNombre(CPConstants.VER_ROLE));
			
			this.userCreator.save(testUser2);
		}
		
		
		log.info("[ADMIN] Destruccion de valores sensibles");
		
	}

}
