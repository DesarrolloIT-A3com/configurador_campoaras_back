package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseSerie 
{
	private String uuid;
	
	private String variante;
	
	private String modulo;
	
	private String extra;
	
	private String uuidProducto;
	
	private String nombre;
	
	private byte [] img;
}
