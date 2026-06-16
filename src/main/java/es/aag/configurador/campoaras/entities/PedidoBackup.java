package es.aag.configurador.campoaras.entities;

import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import es.aag.configurador.campoaras.utils.EstadoPedido;
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
@Table(name="pedido_backup")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PedidoBackup 
{
	@Id
	@Column(nullable=false,unique=true)
	private String uuid;
	
	@Column(nullable=false)
	private String referencia;
	
	@Column(nullable=false)
	private LocalDateTime fecha;
	
	@Column(nullable=false)
	private EstadoPedido estado;
	
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable=false, columnDefinition = "json")
	private List<Map<String,Object>> content = new LinkedList<Map<String,Object>>();
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id",nullable = false)
	private Usuario usuarioPedidoBack;
	
	
}
