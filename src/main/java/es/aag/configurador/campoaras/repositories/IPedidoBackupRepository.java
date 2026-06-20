package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.PedidoBackup;
import es.aag.configurador.campoaras.entities.Usuario;

public interface IPedidoBackupRepository extends JpaRepository<PedidoBackup, String>
{
	public List<PedidoBackup> findByUsuarioPedidoBack(Usuario usuario);
}
