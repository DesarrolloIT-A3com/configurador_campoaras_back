package es.aag.configurador.campoaras.entities;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="usuario")
@Getter
@Setter
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
	private LocalDateTime acceso;
	
	@Column(nullable = false)
	private boolean verificado;
	
	@Column(nullable = false)
	private String USRToken;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Rol rol;
	
	@OneToMany(mappedBy = "usuario")
	private Set<ProductoConfigurado> productos = new HashSet<ProductoConfigurado>();
	
	@OneToMany(mappedBy = "usuarioUuid")
	private Set<BulkProductosUsuario> selecciones = new HashSet<BulkProductosUsuario>();
	
	@OneToMany(mappedBy = "usuarioPedido")
	private Set<Pedido> pedidos = new HashSet<Pedido>();
	
	public void addProducto(ProductoConfigurado producto)
	{
		this.productos.add(producto);
	}
	
	public void removeProducto(ProductoConfigurado producto)
	{
		this.productos.remove(producto);
	}
	
	public void addBulk(BulkProductosUsuario bulk)
	{
		this.selecciones.add(bulk);
	}
	
	public void removeBulk(BulkProductosUsuario bulk)
	{
		this.selecciones.remove(bulk);
	}
	
	public void addPedidos(Pedido pedido)
	{
		this.pedidos.add(pedido);
	}
	
	public void removePedidos(Pedido pedido)
	{
		this.pedidos.remove(pedido);
	}
	
}
