package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Rol;

public interface IRolRepository extends JpaRepository<Rol, String>
{
	Rol findByNombre(String nombre);
}
