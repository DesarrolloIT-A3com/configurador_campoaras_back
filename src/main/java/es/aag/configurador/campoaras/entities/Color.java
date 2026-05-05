package es.aag.configurador.campoaras.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="color")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Color 
{
	@Id
	@Column(nullable = false,unique=true)
	private String uuid;
	
	@Column(nullable = false,unique=true)
	private String nombre;
	
	@ManyToMany(mappedBy = "colores")
	private Set<Acabado> acabados = new HashSet<>();
	
	@OneToMany(mappedBy = "colorArmazon")
	private Set<ProductoConfigurado> colorArmazon = new HashSet<>();
	
	@OneToMany(mappedBy = "colorFrente")
	private Set<ProductoConfigurado> colorFrente = new HashSet<>();
	
	@OneToMany(mappedBy = "colorTirador")
	private Set<ProductoConfigurado> colorTirador = new HashSet<>();
	
	@OneToMany(mappedBy = "colorRegleta")
	private Set<ProductoConfigurado> colorRegleta = new HashSet<>();
	
	
	public void addAcabado(Acabado acabado)
	{
		this.acabados.add(acabado);
		acabado.getColores().add(this);
	}
	
	public void removeAcabado(Acabado acabado)
	{
		this.acabados.remove(acabado);
		acabado.getColores().remove(this);
	}
}
