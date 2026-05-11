package es.aag.configurador.campoaras.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserGetDTO 
{
	private String uuid;
	
	private String email;
	
	private String username;
	
	private float descuento;
	
	private float segundoDescuento;
	
	private String comercial;
	
	private LocalDateTime acceso;
	
	private String rol;
	
	private boolean verificado;
}
