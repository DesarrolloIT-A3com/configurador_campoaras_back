package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Frente;

public interface IFrenteRepository extends JpaRepository<Frente, String>
{
	Frente findByNombre(String nombre);
	
	Frente findByReferencia(String referencia);
}
