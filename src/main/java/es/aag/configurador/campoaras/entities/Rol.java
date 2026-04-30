package es.aag.configurador.campoaras.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rol")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Rol 
{
	@Id
	@Column
	private String uuid;
	
	@Column(nullable = false,unique = true)
	private String nombre;
	
	@OneToMany(mappedBy = "rol")
	private Set<Usuario> usuarios = new HashSet<Usuario>();
}
