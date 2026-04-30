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
@Table
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Verification 
{
	@Id
	@Column(nullable = false, unique = true)
	private String uuid;
	
	@Column(nullable= false, unique = true)
	private String email;
	
	@Column(nullable = false, unique = true)
	private String username;
	
	@Column(nullable = false)
	private String password;
	
	@Column(nullable = false, unique = true)
	private String USRtoken;
	
	@Column(nullable = false)
	private String verCode;
	
	@Column(nullable = false)
	private LocalDateTime endVerCode;
}
