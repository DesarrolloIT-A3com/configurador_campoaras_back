package es.aag.configurador.campoaras.dto;

import java.time.LocalDateTime;
import java.util.List;

import es.aag.configurador.campoaras.utils.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class OrderDTO 
{
	private String uuid;
	
	private String referencia;
	
	private String usuario;
	
	private LocalDateTime fecha;
	
	private String [] productos;
	
	private EstadoPedido estado;
	
	private List<SeleccionDTO> selecciones;
}
