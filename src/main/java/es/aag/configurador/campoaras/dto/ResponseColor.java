package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ResponseColor 
{
	private String uuid;
	
	private String nombre;
	
	private String [] acabados;
	
	private byte [] img;
	
	// Se añade un setter debido a que este campo puede venir nulo
	public void setAcabados(String [] acabados)
	{
		this.acabados = acabados;
	}
}
