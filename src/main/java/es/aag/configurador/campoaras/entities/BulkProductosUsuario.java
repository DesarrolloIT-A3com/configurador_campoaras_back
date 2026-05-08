package es.aag.configurador.campoaras.entities;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bulk_productos_usuario")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BulkProductosUsuario 
{
	@Id
	@Column(nullable = false,unique = true)
	private String uuid;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id",nullable = false)
	private Usuario usuarioUuid;
	
	@Column
	private List<String> productos;
	
	@Column
	private boolean end = false;
	
	@Column
	private LocalDateTime fecha;
}
