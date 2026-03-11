package es.aag.configurador.campoaras.utils;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CPException extends Exception
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -1153037185114323893L;
	
	private int code;
	
	private String message;
	
	private Exception cause;
	
	public CPException(int code,String message)
	{
		super();
		this.code = code;
		this.message = message;
	}
	
	public Map<String,Object> toMap()
	{
		Map<String,Object> response = new HashMap<String, Object>();
		
		response.put("code", (Integer) this.code);
		response.put("message", this.message);
		
		if(this.cause!=null)
		{
			response.put(message, cause.getMessage());
		}
		
		return response;
	}
}

