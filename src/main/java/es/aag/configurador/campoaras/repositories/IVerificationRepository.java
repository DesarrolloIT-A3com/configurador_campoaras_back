package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.Verification;

public interface IVerificationRepository extends JpaRepository<Verification,String>
{
	Verification findByverCode(String verCode);
}
