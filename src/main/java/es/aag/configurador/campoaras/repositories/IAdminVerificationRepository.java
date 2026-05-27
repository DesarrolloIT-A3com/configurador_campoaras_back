package es.aag.configurador.campoaras.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import es.aag.configurador.campoaras.entities.AdminVerification;

public interface IAdminVerificationRepository extends JpaRepository<AdminVerification, String>
{
	public List<AdminVerification> findByAdminUuid(String adminUuid);
}
