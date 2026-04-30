package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.Serie;

public interface IConfiguracionRepository extends JpaRepository<Configuracion, String>
{
	List<Configuracion> findBySerie(Serie serie);
	
}
