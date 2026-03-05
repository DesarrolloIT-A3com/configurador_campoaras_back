package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Configuracion;

public interface IConfiguracionRepository extends JpaRepository<Configuracion, String>
{

}
