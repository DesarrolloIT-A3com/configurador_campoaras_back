package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Usuario;

public interface IUsuarioRepository extends JpaRepository<Usuario, String>
{
	Usuario findByUsername(String username);
	
	Usuario findByEmail(String email);
	
	List<Usuario> findByComercial(String comercial);
}
