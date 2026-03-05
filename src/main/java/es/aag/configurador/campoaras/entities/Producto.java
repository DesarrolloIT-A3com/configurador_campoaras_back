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
import lombok.NoArgsConstructor;

@Entity
@Table(name="producto")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Producto 
{
	@Id
	@Column(nullable = false, unique = true)
	private String uuid;
	
	@Column(nullable = false)
	private String nombre;
	
	@Column(nullable = false)
	private String tipo;
	
	@Column(nullable = false)
	private String cajon;
	
	@OneToMany(mappedBy = "producto")
	private Set<Serie> series = new HashSet<Serie>();
}
