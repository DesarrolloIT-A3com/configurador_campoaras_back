package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.ProductoConfigurado;

public interface IProductoConfiguradoRepository extends JpaRepository<ProductoConfigurado, String>
{

}
