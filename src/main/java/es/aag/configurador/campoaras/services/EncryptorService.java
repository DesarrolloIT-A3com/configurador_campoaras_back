package es.aag.configurador.campoaras.services;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;

@Service
public class EncryptorService 
{
	private final BytesEncryptor encryptor;
	
	public EncryptorService(BytesEncryptor encryptor)
	{
			this.encryptor = encryptor;
	}
	
	 public String encrypt(String attribute) 
	 {
	        if (attribute == null) return null;
	        byte[] encrypted = encryptor.encrypt(attribute.getBytes(StandardCharsets.UTF_8));
	        return Base64.getEncoder().encodeToString(encrypted);
	 }

    public String decrypt(String dbData) 
    {
        if (dbData == null) return null;
        byte[] decoded = Base64.getDecoder().decode(dbData);
        byte[] decrypted = encryptor.decrypt(decoded);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}

