package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseAcabado 
{
	private String uuid;

	private String nombre;
	
	private String [] tipos;
	
	private String [] colores;
	
	private byte[] img;
}
