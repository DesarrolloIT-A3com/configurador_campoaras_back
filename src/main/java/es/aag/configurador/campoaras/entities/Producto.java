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
@Table(name="producto")
@Getter
@Setter
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
	
	@Column(nullable = true)
	private String cajon;
	
	@OneToMany(mappedBy = "producto")
	private Set<Serie> series = new HashSet<Serie>();
	
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(
		name = "producto_frente",
		joinColumns = @JoinColumn(name = "producto_id"),
		inverseJoinColumns = @JoinColumn(name = "frente_id")
		
	)
	private Set<Frente> frentesProductos = new HashSet<Frente>();
	
	public void addFrente(Frente frente)
	{
		this.frentesProductos.add(frente);
		frente.getProductoFrente().add(this);
	}
	
	public void removeProducto(Frente frente)
	{
		this.frentesProductos.remove(frente);
		frente.getProductoFrente().remove(this);
	}
}
