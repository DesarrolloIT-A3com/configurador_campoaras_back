package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Acabado;

public interface IAcabadoREpository extends JpaRepository<Acabado, String>
{
	Acabado findByNombre(String nombre);
}
