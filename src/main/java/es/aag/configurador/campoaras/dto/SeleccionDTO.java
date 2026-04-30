package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SeleccionDTO 
{
	private String uuid;
	
	private String referencia;
	
	private String usuario;
	
	private String serie;
	
	private Float fondo;
	
	private Float ancho;
	
	private Float alto;
	
	private Float precioArmazon;
	
	private String armazon;
	
	private Float precioFrente;
	
	private String frente;
	
	private String acabadoFrente;
	
	private Float precioTirador;
	
	private String acabadoTirador;
	
	private Float precioRegleta;
	
	private String acabadoRegleta;
	
	private Float precioFinal;
	
	private int cantidad;
	
	private Boolean bulk;
}
