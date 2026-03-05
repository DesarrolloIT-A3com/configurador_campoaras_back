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
@Table(name="acabado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Acabado 
{
	@Id
	@Column(nullable = false,unique = true)
	private String uuid;
	
	@Column(nullable = false,unique = true)
	private String nombre;
	
	@Column(nullable = false)
	private String [] tipos;
	
	@OneToMany(mappedBy = "acabado")
	private Set<ProductoConfigurado> acabadoProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "acabadoFrente")
	private Set<ProductoConfigurado> acabadoFrenteProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "acabadoRegleta")
	private Set<ProductoConfigurado> acabadoRegletaProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "acabadoTirador")
	private Set<ProductoConfigurado> acabadoTiradorProductoConfigurado = new HashSet<ProductoConfigurado>();
	
}
