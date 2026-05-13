package es.aag.configurador.campoaras.rest;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import es.aag.configurador.campoaras.dto.AuthDTO;
import es.aag.configurador.campoaras.dto.LoginDTO;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.entities.Verification;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.AuthService;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Controlador encargado de la autenticación de usuario
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@RestController
@RequestMapping("/v1/auth")

public class AuthRestController 
{
	private final Logger log = LogManager.getLogger();
	
	@Autowired
	private AuthService authService;
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	private final GeneralSecurity security;
	
	public AuthRestController()
	{
		this.security = new GeneralSecurity();
	}
	
	@RequestMapping(method = RequestMethod.POST,value = "/register",consumes="application/json")
	public ResponseEntity<?> registerUser(@RequestBody(required = true)final Verification usuario,
			HttpServletRequest request)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			if(usuario.getEmail() == null || usuario.getPassword() == null || usuario.getUsername() == null)
			{
				log.warn("[AVISO] -- /register -- El usuario se ha tratado de registrar con datos faltantes -- {}",seguridad);
				throw new CPException(400,"Datos inválidos");
			}
			
			if(usuario.getEmail().isBlank() || usuario.getPassword().isBlank() || usuario.getUsername().isBlank())
			{
				log.warn("[AVISO] -- /register -- El usuario se ha tratado de registrar con datos en blanco -- {}",seguridad);
				throw new CPException(400,"Datos inválidos");
			}
			
			this.authService.verificate(usuario, seguridad);
			
			
			return ResponseEntity.ok().build();
		}
		
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /register -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/login")
	public ResponseEntity<?> login(@RequestBody(required = true) final LoginDTO body,
			HttpServletRequest request,
			HttpServletResponse response)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			AuthDTO tokens = this.authService.login(body, seguridad);
			
			// ACCESS TOKEN EN COOKIE
			ResponseCookie accessCookie = ResponseCookie.from("access_token",tokens.getAccessToken())
					.httpOnly(true)
					.secure(false)
					.sameSite("Lax")
					.path("/")
					.maxAge(tokens.getAccessExpire()).build();
			
			// REFRESH TOKEN EN COOKIE
			ResponseCookie refreshCookie = ResponseCookie.from("refresh_token",tokens.getRefreshToken())
					.httpOnly(true)
					.secure(false)
					.sameSite("Lax")
					.path("/")
					.maxAge(tokens.getRefreshExpire()).build();
			
			response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
			response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
			
			return ResponseEntity.ok().build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /login -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/logout")
	public ResponseEntity<?> logout(
			HttpServletRequest request,
			HttpServletResponse response,
			Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
		    Usuario usuario = this.security.isAuth(this.userRepo,"/logout" , seguridad);
		   
			// Invalida el access token
	        ResponseCookie accessCookie = ResponseCookie.from("access_token", "")
	                .httpOnly(true)
	                .secure(false)   // Igual que en login
	                .sameSite("Lax")
	                .path("/")
	                .maxAge(0)       // Esto elimina la cookie del navegador
	                .build();

	        // Invalida el refresh token
	        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", "")
	                .httpOnly(true)
	                .secure(false)
	                .sameSite("Lax")
	                .path("/")
	                .maxAge(0)
	                .build();

	        
	        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
	        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

	        log.info("[ACCION] -- /logout -- {} Ha solicitado un logout con un jwt valido -- {}",usuario.getUSRToken(),seguridad);
	        
	        return ResponseEntity.ok().build();
			
			
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			
			
			String seguridad = this.security.getIpInfo(ip, request);
			log.error("[ERROR] -- /logout -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");		
		}
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/me",produces = "application/json")
	public ResponseEntity<?> authMe(HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Map<String,Object> response = this.authService.authUser(authentication, seguridad);
			
			return ResponseEntity.ok().body(response);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /me -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response)
	{
	    try
	    {
	        String ip = this.security.getClientIPAddress(request);
	        String seguridad = this.security.getIpInfo(ip, request);

	        AuthDTO tokens = this.authService.refresh(request, seguridad);

	        // Nuevo access token en cookie
	        ResponseCookie accessCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
	                .httpOnly(true)
	                .secure(false)
	                .sameSite("Lax")
	                .path("/")
	                .maxAge(tokens.getAccessExpire()).build();
	        
	     // REFRESH TOKEN EN COOKIE
		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token",tokens.getRefreshToken())
				.httpOnly(true)
				.secure(false)
				.sameSite("Lax")
				.path("/")
				.maxAge(tokens.getRefreshExpire()).build();

	        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
	        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

	        return ResponseEntity.ok().build();
	    }
	    catch(CPException ex)
	    {
	        return ResponseEntity.status(ex.getCode()).body(ex.toMap());
	    }
	    catch(Exception ex)
	    {
	        String ip = this.security.getClientIPAddress(request);
	        String seguridad = this.security.getIpInfo(ip, request);

	        log.error("[ERROR] -- /refresh -- Error interno de servidor -- {} -- {}", ex.getMessage(), seguridad);
	        log.error("[DETAILS]", ex);
	        return ResponseEntity.status(500).body("Error interno de servidor");
	    }
	}
	
	@RequestMapping(method = RequestMethod.GET,value = "/capabilities",produces = "application/json")
	public ResponseEntity<?> getCapabilities(HttpServletRequest request,Authentication authentication)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			Usuario usuario = this.security.isAuth(userRepo, "/capabilities", seguridad);
			
			Map<String,Boolean> capabilities = this.authService.getCapabilities(usuario.getRol(), seguridad, "/capabilities", usuario.getUSRToken());
			
			return ResponseEntity.ok().body(capabilities);
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /capabilities -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");
		}
	}

	
	@RequestMapping(method = RequestMethod.POST, value = "/verify/{verificationCode}")
	public ResponseEntity<?> verificate (@PathVariable(value = "verificationCode",required = true) final String verificationCode,
			HttpServletRequest request)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			this.authService.register(verificationCode, seguridad);
			
			return ResponseEntity.status(201).build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /verify -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");
		}
	}
	
	
	
	@RequestMapping(method = RequestMethod.POST, value = "/forget-password/{userEmail}")
	public ResponseEntity<?> forgetPass (@PathVariable(value = "userEmail",required = true) final String userEmail,
			HttpServletRequest request)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			this.authService.verificateForgetPass(userEmail, seguridad);
			
			return ResponseEntity.ok().build();
		}
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /verify -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");
		}
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/reset-password/{verificationCode}",consumes = "application/json")
	public ResponseEntity<?> verificate (@PathVariable(value = "verificationCode",required = true) final String verificationCode,
			@RequestBody(required = true) Map<String,String> body,
			HttpServletRequest request)
	{
		try
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			String newPass = body.getOrDefault("newPassword",CPConstants.MAP_DEFAULT_VALUE);
			
			this.authService.changePass(verificationCode, newPass, seguridad);
			
			return ResponseEntity.ok().build();
		}	
		catch(CPException ex)
		{
			return ResponseEntity.status(ex.getCode()).body(ex.toMap());
		}
		catch(Exception ex)
		{
			String ip = this.security.getClientIPAddress(request);
			String seguridad = this.security.getIpInfo(ip, request);
			
			log.error("[ERROR] -- /verify -- Error interno de servidor -- {} -- {}",ex.getMessage(),seguridad);
			log.error("[DETAILS]",ex);
			return ResponseEntity.status(500).body("Error interno de servidor");
		}
	}
	
	
		
	
}
