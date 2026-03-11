package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Serie;

public interface ISerieRepository extends JpaRepository<Serie, String>
{
	Serie findByVariante(String variante);
}
