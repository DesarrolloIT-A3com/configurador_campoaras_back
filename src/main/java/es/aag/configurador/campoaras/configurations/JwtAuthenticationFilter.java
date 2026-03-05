package es.aag.configurador.campoaras.configurations;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter
{
	private final Logger log = LogManager.getLogger();
	private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService)
    {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException 
    {
    	
    	GeneralSecurity security = new GeneralSecurity();
    	String ip = security.getClientIPAddress(request);
		String seguridad = security.getIpInfo(ip, request);

        String userUuid = null;
        String jwt = null;

        
        // Extraccion de JWT mediante cookie
        if(request.getCookies() != null)
        {
        	for(Cookie cookie:request.getCookies())
        	{
        		if("access_token".equals(cookie.getName()))
        		{
        			jwt = cookie.getValue();
        			break;
        		}
        	}
        }

        // Validación de JWT solo si existe y no hay auth previa
        if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) 
        {
        	try
        	{
                if (jwtUtil.validateToken(jwt))
                {
                	userUuid = jwtUtil.extractSubject(jwt);
                    var userDetails = this.userDetailsService.loadUserByUsername(userUuid);
                    
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(userUuid, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
                else
                {
                    log.info("[AVISO] Se ha usado un token invalido -- {}", seguridad);
                }
            }
            catch(ExpiredJwtException ex)
            {
                log.info("[AVISO] Se ha usado un token expirado -- {}", seguridad);
            }
            catch(Exception ex)
            {
                log.warn("[AVISO] Error inesperado validando token -- {}", seguridad);
                log.error("[DETAILS]", ex);
            }
            
        }
       
        
        chain.doFilter(request, response);
        
    }
    
}
