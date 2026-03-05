package es.aag.configurador.campoaras.entities;

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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name="serie")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Serie 
{
	@Id
	@Column(nullable = false, unique = true)
	private String uuid;
	
	@Column(nullable = false)
	private String nombre;
	
	@Column(nullable = false)
	private String modulo;
	
	@Column(nullable = false)
	private String extra;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "producto_id")
    private Producto producto;
	
	@OneToMany(mappedBy = "serie")
	private Set<Configuracion> configuracion = new HashSet<Configuracion>();
}
