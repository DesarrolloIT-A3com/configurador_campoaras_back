package es.aag.configurador.campoaras.dto;

import org.springframework.core.io.Resource;

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
	
	private byte [] img;
}
