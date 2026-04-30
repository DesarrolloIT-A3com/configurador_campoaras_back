package es.aag.configurador.campoaras.entities;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="frente")
@Getter
@Setter
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
	
	@Column(nullable = false)
    private boolean regleta;
    
    @Column(nullable = false)
    private boolean tirador;
	
	@ManyToMany(mappedBy = "frentes", fetch = FetchType.LAZY)
	private Set<Acabado> acabados = new HashSet<>();
	
	@ManyToMany(mappedBy = "frentesExtension",fetch = FetchType.LAZY)
	private Set<Acabado> acabadosExtension = new HashSet<>();
	
	@ManyToMany(mappedBy = "frentesProductos",fetch = FetchType.LAZY)
	private Set<Producto> productoFrente = new HashSet<>();
	
	@OneToMany(mappedBy = "frente")
	private Set<ProductoConfigurado> productoConfigurado = new HashSet<ProductoConfigurado>();

	public void addAcabado(Acabado acabado)
	{
		this.acabados.add(acabado);
		acabado.getFrentes().add(this);
	}
	
	public void removeAcabado(Acabado acabado)
	{
		this.acabados.remove(acabado);
		acabado.getFrentes().remove(this);
	}
	
	public void addAcabadoExtension(Acabado acabado)
	{
		this.acabadosExtension.add(acabado);
		acabado.getFrentesExtension().add(this);
	}
	
	public void removeAcabadoExtension(Acabado acabado)
	{
		this.acabadosExtension.remove(acabado);
		acabado.getFrentesExtension().remove(this);
	}
	
	public void addProducto(Producto producto)
	{
		this.productoFrente.add(producto);
		producto.getFrentesProductos().add(this);
	}
	
	public void removeProducto(Producto producto)
	{
		this.productoFrente.remove(producto);
		producto.getFrentesProductos().remove(this);
	}
}
