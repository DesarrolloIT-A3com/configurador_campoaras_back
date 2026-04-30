package es.aag.configurador.campoaras.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ResponseConfiguracion 
{
	private String referencia;
		
	private float fondo;
	
	private float ancho;
	
	private float alto;
	
	private float altoMax;
	
	private float fondoMin;
	
	private float fondoMax;
	
	private float precioMedidaFondoEsp;
	
	private float precioMedidaAnchoEsp;
	
	private float precioMedidaAltoEsp;
	
	private List<Map<String,Object>> armazon;
	
	private List<Map<String,Object>> extras;
	
	private String serie;
}
