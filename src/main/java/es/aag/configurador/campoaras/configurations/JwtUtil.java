package es.aag.configurador.campoaras.configurations;

import java.security.Key;
import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil 
{
	@Value("${CPWT_KEY}")
	private String SECRET_KEY;
	private final long ACC_EXP_TIME = 10 * 60 * 1000; // 10 minutos
	private final long REF_EXP_TIME = 7 * 24 * 60 * 60 * 1000; // 7 días
	
	private Key getSigningKey()
	{
		return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
	}
	
	public String generateToken(String token)
	{
        return Jwts.builder()
                .setSubject(token)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACC_EXP_TIME))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
	
	public String generateRefreshToken(String uuid)
	{
		return Jwts.builder()
				.setSubject(uuid)
				.setIssuedAt(new Date())
				.setExpiration(new Date(System.currentTimeMillis() + REF_EXP_TIME))
				.signWith(getSigningKey(),SignatureAlgorithm.HS256)
				.compact();
	}
	
	 public String extractSubject(String token) 
	 {
	        return Jwts.parserBuilder()
	                .setSigningKey(getSigningKey())
	                .build()
	                .parseClaimsJws(token)
	                .getBody()
	                .getSubject();
	 }

	 public boolean validateToken(String token)
	 {
	     try
	     {
	         Jwts.parserBuilder()
	             .setSigningKey(getSigningKey())
	             .build()
	             .parseClaimsJws(token);
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
    		Date expiration = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getExpiration();
            return expiration.before(new Date());
    	}
    	catch(Exception ex)
    	{
    		return true;
    	}
    }
}
