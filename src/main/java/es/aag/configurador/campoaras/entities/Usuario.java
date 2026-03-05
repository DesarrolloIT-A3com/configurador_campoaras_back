package es.aag.configurador.campoaras.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="usuario")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Usuario 
{
	@Id
	@Column(nullable = false,unique = true)
	private String uuid;
	
	@Column(nullable = false,unique = true)
	private String email;
	
	@Column(nullable = false)
	private String username;
	
	@Column(nullable = false)
	private String password;
	
	@Column(nullable = false)
	private float descuento;
	
	// Este campo hace referencia al email del comercial
	@Column
	private String comercial;
	
	@Column
	private String USRToken;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Rol rol;
	
}
