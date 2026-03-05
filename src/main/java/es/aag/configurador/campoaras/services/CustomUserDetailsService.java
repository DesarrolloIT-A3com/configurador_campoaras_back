package es.aag.configurador.campoaras.services;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService
{
	private Logger log = LogManager.getLogger();
	
	private final IUsuarioRepository userRepo;
	
	public CustomUserDetailsService(IUsuarioRepository userRepo)
	{
		this.userRepo = userRepo;
	}
	
	@Override
    public UserDetails loadUserByUsername(String uuid) throws UsernameNotFoundException 
	{
		
		Optional<Usuario> optUsuario = this.userRepo.findById(uuid);
        
        if(!optUsuario.isPresent())
        {
        	log.error("[AVISO] Intento de acceso fraudulento");
        	throw new UsernameNotFoundException("Usuario no encontrado");
        }
        
        Usuario usuario = optUsuario.get();
        
        UserDetails user = User.withUsername(usuario.getUsername())
        					.password(usuario.getPassword())
        					.roles(usuario.getRol().getNombre())
        					.build();
        return user;
    }
	
}
