package es.aag.configurador.campoaras.configurations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil 
{
	@Value("${CPWT_KEY}")
	private String SECRET_KEY;
	private final long ACC_EXP_TIME = 10 * 60 * 1000; // 10 minutos
	private final long REF_EXP_TIME = 3 * 60 * 60 * 1000; // 3 horas
	
	private SecretKey getSigningKey()
	{
		try
		{
			MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
			byte[] normalizedKeyBytes = sha256.digest(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
			return Keys.hmacShaKeyFor(normalizedKeyBytes); // siempre 32 bytes -> HmacSHA256
		}
		catch (NoSuchAlgorithmException e)
		{
			// SHA-256 es un algoritmo obligatorio en cualquier JVM estandar; no deberia ocurrir nunca.
			throw new IllegalStateException("SHA-256 no disponible en esta JVM", e);
		}
	}
	
	public String generateToken(String token)
	{
        return Jwts.builder()
                .subject(token)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACC_EXP_TIME))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }
	
	public String generateRefreshToken(String uuid)
	{
		return Jwts.builder()
				.subject(uuid)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + REF_EXP_TIME))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
				.compact();
	}
	
	 public String extractSubject(String token) 
	 {
	        return Jwts.parser()
	                .verifyWith(getSigningKey())
	                .build()
	                .parseSignedClaims(token)
	                .getPayload()
	                .getSubject();
	 }

	 public boolean validateToken(String token)
	 {
	     try
	     {
	         Jwts.parser()
	             .verifyWith(getSigningKey())
	             .build()
	             .parseSignedClaims(token);
	         return true;
	     }
	     catch (ExpiredJwtException e)
	     {
	         throw e; // Se lanza esta excepcion para que el filtro sepa que ha expirado
	     }
	     catch (JwtException | IllegalArgumentException e)
	     {
	         return false;
	     }
	 }
    
    public boolean isTokenExpired(String token)
    {
    	try
    	{
    		Date expiration = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
    	}
    	catch(Exception ex)
    	{
    		return true;
    	}
    }
}
