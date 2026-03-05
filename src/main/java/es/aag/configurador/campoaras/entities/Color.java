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
@Table(name="color")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Color 
{
	@Id
	@Column(nullable = false,unique=true)
	private String uuid;
	
	@Column(nullable = false,unique=true)
	private String nombre;
	
	@OneToMany(mappedBy = "colorArmazon")
	private Set<Configuracion> colorArmazonConfiguracion = new HashSet<Configuracion>();
	
	@OneToMany(mappedBy = "colorFrente")
	private Set<Configuracion> colorFrenteConfiguracion = new HashSet<Configuracion>();
	
	@OneToMany(mappedBy = "colorTirador")
	private Set<Configuracion> colorTiradorConfiguracion = new HashSet<Configuracion>();
	
	@OneToMany(mappedBy = "colorRegleta")
	private Set<Configuracion> colorRegletaConfiguracion = new HashSet<Configuracion>();
	
	@OneToMany(mappedBy = "colorAcabado")
	private Set<ProductoConfigurado> colorAcabadoProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "colorFrente")
	private Set<ProductoConfigurado> colorFrenteProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "colorRegleta")
	private Set<ProductoConfigurado> colorRegletaProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "colorTirador")
	private Set<ProductoConfigurado> colorTiradorProductoConfigurado = new HashSet<ProductoConfigurado>();
}
