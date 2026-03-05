package es.aag.configurador.campoaras;

import java.security.SecureRandom;
import java.util.Base64;

public class RandomCodeGenerator {

	private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    public static String generate32BitCode() {
        byte[] randomBytes = new byte[8]; // 64 bits
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }
    
    public static void generateHexCode()
    {
    	int bytesLength = 16; // 16 bytes = 128 bits
        byte[] salt = new byte[bytesLength];

        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(salt);

        StringBuilder hexSalt = new StringBuilder();
        for (byte b : salt) {
            hexSalt.append(String.format("%02x", b));
        }

        System.out.println("Generated HEX Salt (32 chars):");
        System.out.println(hexSalt.toString());
    }

    public static void main(String[] args) {
//    	String code = "";
//    	for (int i = 0; i < 5; i++) {
//    		code += generate32BitCode();
//        }
//    	
//    	System.out.println(code);
    	generateHexCode();
    }

}
