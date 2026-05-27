package es.aag.configurador.campoaras.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="admin_verification")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AdminVerification 
{
	@Id
	@Column(nullable = false,unique = true)
	private String uuid;
	
	@Column(nullable = false)
	private String adminUuid;
	
	@Column(nullable = false)
	private String verCode;
	
	@Column(nullable = false)
	private LocalDateTime endVerCode;
}
