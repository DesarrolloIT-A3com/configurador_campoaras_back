package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseProducto 
{
	
	private String uuid;
	
	private String nombre;
		
	private String tipo;
	
	private String cajon;
	
	private int orden;
	
	private byte [] img;
}
