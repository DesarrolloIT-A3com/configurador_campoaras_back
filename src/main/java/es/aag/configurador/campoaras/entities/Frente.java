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
@Table(name="frente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Frente 
{
	@Id
	@Column(nullable = false,unique = true)
	private String uuid;
	
	@Column(nullable = false,unique = true)
	private String nombre;
	
	@Column(nullable = false,unique = true)
	private String referencia;
	
	@OneToMany(mappedBy = "frente")
	private Set<Configuracion> configuracion = new HashSet<Configuracion>();
	
	@OneToMany(mappedBy = "frente")
	private Set<ProductoConfigurado> productoConfigurado = new HashSet<ProductoConfigurado>();

	
}
