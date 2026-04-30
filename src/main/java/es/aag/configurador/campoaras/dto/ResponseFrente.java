package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseFrente 
{
	private String uuid;
	
	private String nombre;
	
	private String referencia;
	
	private boolean regleta;
	
	private boolean tirador;
	
	private String [] acabados;
	
	private String [] acabadosExtension;
	
	private String [] productos;
	
	private byte[] img;
}
