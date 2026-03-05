package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Producto;

public interface IProductoRepository extends JpaRepository<Producto, String>
{
	Producto findByNombre(String nombre);
}
