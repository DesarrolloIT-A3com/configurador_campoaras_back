package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;

public interface IProductoConfiguradoRepository extends JpaRepository<ProductoConfigurado, String>
{
	List<ProductoConfigurado> findByConfiguracion(Configuracion configuracion);
}
