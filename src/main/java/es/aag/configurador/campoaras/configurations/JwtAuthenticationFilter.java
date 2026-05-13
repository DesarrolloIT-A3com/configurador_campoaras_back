package es.aag.configurador.campoaras.configurations;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import es.aag.configurador.campoaras.security.GeneralSecurity;
import es.aag.configurador.campoaras.services.CustomUserDetailsService;
import es.aag.configurador.campoaras.utils.CPConstants;
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
    private final List<String> PUBLIC_ROUTES = Arrays.asList(CPConstants.ROUTES_JWT);


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
    	// Si es ruta pública se pasa sin tocar la cookie para evitar filtros falsos
    	if(this.isPublicRoute(request.getRequestURI()))
    	{
    		chain.doFilter(request, response);
    		return;
    	}
    	
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
        
        if(jwt == null)
        {
        	chain.doFilter(request, response);
        	return;
        }

        // Validación de JWT solo si existe y no hay auth previa
        if (SecurityContextHolder.getContext().getAuthentication() == null) 
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
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 403, "Token expirado");
                    return;
                }
                catch(Exception ex)
                {
                    log.warn("[AVISO] Error inesperado validando token -- {}", seguridad);
                    log.error("[DETAILS]", ex);
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, 403, "Token inválido");
                    return;
                }
            
        }
       
        
        chain.doFilter(request, response);
        
    }
    
    private void sendErrorResponse(HttpServletResponse response, int httpStatus, int code, String message)
            throws IOException
    {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            String.format("{\"code\":%d,\"message\":\"%s\"}", code, message)
        );
    }

	private boolean isPublicRoute(String requestUri)
    {	
    	return PUBLIC_ROUTES.stream().anyMatch(route -> 
    	{
    		if (route.endsWith("/**"))
            {
                String base = route.substring(0, route.length() - 3);
                return requestUri.startsWith(base);
            }
            return route.equals(requestUri);
    	});
    }
    
}
