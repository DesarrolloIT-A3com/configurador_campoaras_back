package es.aag.configurador.campoaras.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.VerificationPass;

public interface IVerificationPassRepository extends JpaRepository<VerificationPass,String>
{
	VerificationPass findByverCode(String verCode);
	
	VerificationPass findByEmail(String email);
}
