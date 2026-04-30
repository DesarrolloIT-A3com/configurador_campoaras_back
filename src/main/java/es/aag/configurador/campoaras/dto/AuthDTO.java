package es.aag.configurador.campoaras.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthDTO 
{
	private String accessToken;
	
	private String refreshToken;
	
	private long accessExpire;
	
	private long refreshExpire;
}
