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
	
	private String colorArmazon;
	
	private Float precioFrente;
	
	private String frente;
	
	private String acabadoFrente;
	
	private String colorFrente;
	
	private Float precioTirador;
	
	private String acabadoTirador;
	
	private String colorTirador;
	
	private Float precioRegleta;
	
	private String acabadoRegleta;
	
	private String colorRegleta;
	
	private Float precioFinal;
	
	private int cantidad;
	
	private Boolean bulk;

	private String referenciaBulk;
}
