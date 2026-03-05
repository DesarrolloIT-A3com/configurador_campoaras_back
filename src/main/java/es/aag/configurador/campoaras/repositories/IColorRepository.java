package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Color;

public interface IColorRepository extends JpaRepository<Color, String>
{
	Color findByNombre(String nombre);
}
