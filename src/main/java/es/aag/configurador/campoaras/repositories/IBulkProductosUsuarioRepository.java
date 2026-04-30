package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.Usuario;

public interface IBulkProductosUsuarioRepository extends JpaRepository<BulkProductosUsuario, String>
{
	public List<BulkProductosUsuario> findByUsuarioUuid(Usuario usuario);
	
	public BulkProductosUsuario findByUsuarioUuidAndEnd(Usuario usuario,boolean end);
	
	public BulkProductosUsuario findByEnd(boolean end);
}
