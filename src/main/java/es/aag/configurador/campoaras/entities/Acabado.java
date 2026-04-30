package es.aag.configurador.campoaras.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="acabado")
@Getter
@Setter
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
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "acabado_color",
		joinColumns = @JoinColumn(name = "acabado_id"),
		inverseJoinColumns = @JoinColumn(name = "color_id")
	)
	private Set<Color> colores = new HashSet<Color>();
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "frente_acabado",
		joinColumns = @JoinColumn(name = "frente_id"),
		inverseJoinColumns = @JoinColumn(name = "acabado_id")
	)
	private Set<Frente> frentes = new HashSet<Frente>();
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "regleta_tirador_acabado",
		joinColumns = @JoinColumn(name = "frente_id"),
		inverseJoinColumns = @JoinColumn(name = "acabado_id")
	)
	private Set<Frente> frentesExtension = new HashSet<Frente>();
	
	
	@OneToMany(mappedBy = "acabado")
	private Set<ProductoConfigurado> acabadoProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "acabadoFrente")
	private Set<ProductoConfigurado> acabadoFrenteProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "acabadoRegleta")
	private Set<ProductoConfigurado> acabadoRegletaProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "acabadoTirador")
	private Set<ProductoConfigurado> acabadoTiradorProductoConfigurado = new HashSet<ProductoConfigurado>();
	
	public void addColor(Color color)
	{
		this.colores.add(color);
		color.getAcabados().add(this);
	}
	
	public void removeColor(Color color)
	{
		this.colores.remove(color);
		color.getAcabados().remove(this);
	}
	
	public void addFrente(Frente frente)
	{
		this.frentes.add(frente);
		frente.getAcabados().add(this);
	}
	
	public void removeFrente(Frente frente)
	{
		this.frentes.remove(frente);
		frente.getAcabados().remove(this);
	}
	
	public void addFrenteExtension(Frente frente)
	{
		this.frentesExtension.add(frente);
		frente.getAcabadosExtension().add(this);
	}
	
	public void removeFrenteExtension(Frente frente)
	{
		this.frentesExtension.remove(frente);
		frente.getAcabadosExtension().remove(this);
	}
}
