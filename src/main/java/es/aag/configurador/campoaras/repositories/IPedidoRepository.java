package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Pedido;
import es.aag.configurador.campoaras.entities.Usuario;

public interface IPedidoRepository extends JpaRepository<Pedido, String>
{
	public List<Pedido> findByReferencia(String referencia);
	
	public List<Pedido> findByUsuarioPedido(Usuario usuario);
}
