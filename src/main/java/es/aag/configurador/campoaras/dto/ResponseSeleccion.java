package es.aag.configurador.campoaras.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseSeleccion 
{
	private String uuid;
	
	private String usuario;
	
	private String referencia;
	
	private SeleccionDTO [] selecciones;
	
	private LocalDateTime fecha;
	
	private boolean finished;
}
