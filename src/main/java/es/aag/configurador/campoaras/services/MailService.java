package es.aag.configurador.campoaras.services;

import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import jakarta.mail.MessagingException;

/**
 * Servicio encargado de enviar correos a nombre del correo de administración o destinado para la app, los html se guardan en método
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@Service
public class MailService 
{
	private static Logger log = LogManager.getLogger();
	
	@Autowired
	private JavaMailSender javaMailSender;
	
	private final String sender;
	

	public MailService() 
	{
		this.sender = CPConstants.ADMIN_MAIL;
	}
	
	public void sendMail(String username,String receiver,String usrToken,String seguridad,String verCode) throws CPException
	{
		String subject = "Verificacion de cuenta";
		String htmlContent = String.format(
				"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"<meta charset=\"utf-8\">\n" +
				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
				"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
				"\n" +
				"<style>\n" +
				"@import url('https://fonts.googleapis.com/css2?family=Lexend&display=swap');\n" +
				"\n" +
				"h1, h2, h3, h4, h5, h6, p, label, span, input, button, option, details, summary, strong, li, figcaption, b {\n" +
				"    font-family: 'Lexend', Arial, sans-serif;\n" +
				"    letter-spacing: 2px;\n" +
				"    color: black;\n" +
				"    margin: 0;\n" +
				"    padding: 0;\n" +
				"}\n" +
				"\n" +
				"a {\n" +
				"    color: #5026b3;\n" +
				"}\n" +
				"\n" +
				"a:active {\n" +
				"    color: #87116e;\n" +
				"}\n" +
				"\n" +
				".card {\n" +
				"    width: 100%%;\n" +
				"    max-width: 600px;\n" +
				"    height: auto;\n" +
				"    min-height: 400px;\n" +
				"    text-align: center;\n" +
				"    background-color: rgba(103, 224, 158, 0.7);\n" +
				"    padding: 40px 20px;\n" +
				"    border: 10px solid #2b0091;\n" +
				"    margin: 0 auto;\n" +
				"    box-sizing: border-box;\n" +
				"}\n" +
				"\n" +
				".code {\n" +
				"    border-radius: 5px;\n" +
				"    padding: 15px 20px;\n" +
				"    letter-spacing: 8px;\n" +
				"    background-color: lightgrey;\n" +
				"    color: black;\n" +
				"    border: 2px solid #2b0091;\n" +
				"    text-align: center;\n" +
				"    display: inline-block;\n" +
				"    font-size: 24px;\n" +
				"    font-weight: bold;\n" +
				"    margin: 30px auto;\n" +
				"    line-height: 1.2;\n" +
				"}\n" +
				"\n" +
				".welcome-title {\n" +
				"    font-size: 28px;\n" +
				"    margin-top: 40px;\n" +
				"    margin-bottom: 20px;\n" +
				"    font-weight: bold;\n" +
				"    line-height: 1.3;\n" +
				"    color: black;\n" +
				"}\n" +
				"\n" +
				".instruction {\n" +
				"    font-size: 18px;\n" +
				"    margin-bottom: 40px;\n" +
				"    line-height: 1.4;\n" +
				"    padding: 0 10px;\n" +
				"    color: black;\n" +
				"}\n" +
				"\n" +
				"@media only screen and (max-width: 620px) {\n" +
				"\n" +
				"    .card {\n" +
				"        max-width: 100%% !important;\n" +
				"        width: 100%% !important;\n" +
				"        min-height: 350px !important;\n" +
				"        padding: 30px 15px !important;\n" +
				"        border: 8px solid #2b0091 !important;\n" +
				"    }\n" +
				"\n" +
				"    .welcome-title {\n" +
				"        font-size: 24px !important;\n" +
				"        margin-top: 30px !important;\n" +
				"        margin-bottom: 15px !important;\n" +
				"        letter-spacing: 1px !important;\n" +
				"    }\n" +
				"\n" +
				"    .instruction {\n" +
				"        font-size: 16px !important;\n" +
				"        margin-bottom: 30px !important;\n" +
				"        letter-spacing: 1px !important;\n" +
				"    }\n" +
				"\n" +
				"    .code {\n" +
				"        font-size: 18px !important;\n" +
				"        padding: 12px 15px !important;\n" +
				"        letter-spacing: 6px !important;\n" +
				"        margin: 25px auto !important;\n" +
				"    }\n" +
				"\n" +
				"}\n" +
				"\n" +
				"@media only screen and (max-width: 480px) {\n" +
				"\n" +
				"    .welcome-title {\n" +
				"        font-size: 18px !important;\n" +
				"    }\n" +
				"\n" +
				"    .instruction {\n" +
				"        font-size: 12px !important;\n" +
				"    }\n" +
				"\n" +
				"    .code {\n" +
				"        font-size: 16px !important;\n" +
				"        letter-spacing: 4px !important;\n" +
				"        padding: 10px 12px !important;\n" +
				"    }\n" +
				"\n" +
				"}\n" +
				"</style>\n" +
				"\n" +
				"</head>\n" +
				"\n" +
				"<body style=\"margin:0; padding:20px; background-color:#f0f0f0;\">\n" +
				"\n" +
				"<center>\n" +
				"<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%%\" style=\"max-width:600px; margin:0 auto;\">\n" +
				"<tr>\n" +
				"<td style=\"padding:0;\">\n" +
				"\n" +
				"<div class=\"card\">\n" +
				"\n" +
				"<div class=\"welcome-title\">\n" +
				"Bienvenido/a %s al presupuestador de Campoaras\n" +
				"</div>\n" +
				"\n" +
				"<div class=\"instruction\">\n" +
				"Para verificar tu cuenta introduce el siguiente código:\n" +
				"</div>\n" +
				"\n" +
				"<div class=\"code\">\n" +
				"%s\n" +
				"</div>\n" +
				"\n" +
				"<div class=\"instruction\">\n" +
				"El código expirará en 5 minutos\n" +
				"</div>\n" +
				"\n" +
				"</div>\n" +
				"\n" +
				"</td>\n" +
				"</tr>\n" +
				"</table>\n" +
				"</center>\n" +
				"\n" +
				"</body>\n" +
				"</html>",
				username, verCode
				);
		try
		{
			var mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,"UTF-8");
			
			helper.setFrom(this.sender,"Campoaras");
			helper.setTo(receiver);
			helper.setSubject(subject);
			helper.setText(htmlContent,true);
			
			this.javaMailSender.send(mimeMessage);
			
			log.info("[ADMIN] -- /register -- Correo de verificacion de registro enviado con exito a {} -- {}",usrToken,seguridad);
		}
		catch(MailException | MessagingException |  UnsupportedEncodingException exception)
		{
			log.error("[ERROR] No se ha podido enviar el mensaje de correo a {} - {} - causa {}",usrToken,seguridad,exception);
			throw new CPException(500,"Error interno del servidor, problemas al enviar mail",exception);
		}
	}
	
	public void sendMailForgetPass(String username,String receiver,String usrToken,String seguridad,String verCode) throws CPException
	{
		String subject = "Verificacion de cuenta";
		String htmlContent = String.format(
				"<!DOCTYPE html>\n" +
				"<html>\n" +
				"<head>\n" +
				"<meta charset=\"utf-8\">\n" +
				"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n" +
				"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
				"\n" +
				"<style>\n" +
				"@import url('https://fonts.googleapis.com/css2?family=Lexend&display=swap');\n" +
				"\n" +
				"h1, h2, h3, h4, h5, h6, p, label, span, input, button, option, details, summary, strong, li, figcaption, b {\n" +
				"    font-family: 'Lexend', Arial, sans-serif;\n" +
				"    letter-spacing: 2px;\n" +
				"    color: black;\n" +
				"    margin: 0;\n" +
				"    padding: 0;\n" +
				"}\n" +
				"\n" +
				"a {\n" +
				"    color: #5026b3;\n" +
				"}\n" +
				"\n" +
				"a:active {\n" +
				"    color: #87116e;\n" +
				"}\n" +
				"\n" +
				".card {\n" +
				"    width: 100%%;\n" +
				"    max-width: 600px;\n" +
				"    height: auto;\n" +
				"    min-height: 400px;\n" +
				"    text-align: center;\n" +
				"    background-color: rgba(103, 224, 158, 0.7);\n" +
				"    padding: 40px 20px;\n" +
				"    border: 10px solid #2b0091;\n" +
				"    margin: 0 auto;\n" +
				"    box-sizing: border-box;\n" +
				"}\n" +
				"\n" +
				".code {\n" +
				"    border-radius: 5px;\n" +
				"    padding: 15px 20px;\n" +
				"    letter-spacing: 8px;\n" +
				"    background-color: lightgrey;\n" +
				"    color: black;\n" +
				"    border: 2px solid #2b0091;\n" +
				"    text-align: center;\n" +
				"    display: inline-block;\n" +
				"    font-size: 24px;\n" +
				"    font-weight: bold;\n" +
				"    margin: 30px auto;\n" +
				"    line-height: 1.2;\n" +
				"}\n" +
				"\n" +
				".welcome-title {\n" +
				"    font-size: 28px;\n" +
				"    margin-top: 40px;\n" +
				"    margin-bottom: 20px;\n" +
				"    font-weight: bold;\n" +
				"    line-height: 1.3;\n" +
				"    color: black;\n" +
				"}\n" +
				"\n" +
				".instruction {\n" +
				"    font-size: 18px;\n" +
				"    margin-bottom: 40px;\n" +
				"    line-height: 1.4;\n" +
				"    padding: 0 10px;\n" +
				"    color: black;\n" +
				"}\n" +
				"\n" +
				"@media only screen and (max-width: 620px) {\n" +
				"\n" +
				"    .card {\n" +
				"        max-width: 100%% !important;\n" +
				"        width: 100%% !important;\n" +
				"        min-height: 350px !important;\n" +
				"        padding: 30px 15px !important;\n" +
				"        border: 8px solid #2b0091 !important;\n" +
				"    }\n" +
				"\n" +
				"    .welcome-title {\n" +
				"        font-size: 24px !important;\n" +
				"        margin-top: 30px !important;\n" +
				"        margin-bottom: 15px !important;\n" +
				"        letter-spacing: 1px !important;\n" +
				"    }\n" +
				"\n" +
				"    .instruction {\n" +
				"        font-size: 16px !important;\n" +
				"        margin-bottom: 30px !important;\n" +
				"        letter-spacing: 1px !important;\n" +
				"    }\n" +
				"\n" +
				"    .code {\n" +
				"        font-size: 18px !important;\n" +
				"        padding: 12px 15px !important;\n" +
				"        letter-spacing: 6px !important;\n" +
				"        margin: 25px auto !important;\n" +
				"    }\n" +
				"\n" +
				"}\n" +
				"\n" +
				"@media only screen and (max-width: 480px) {\n" +
				"\n" +
				"    .welcome-title {\n" +
				"        font-size: 18px !important;\n" +
				"    }\n" +
				"\n" +
				"    .instruction {\n" +
				"        font-size: 12px !important;\n" +
				"    }\n" +
				"\n" +
				"    .code {\n" +
				"        font-size: 16px !important;\n" +
				"        letter-spacing: 4px !important;\n" +
				"        padding: 10px 12px !important;\n" +
				"    }\n" +
				"\n" +
				"}\n" +
				"</style>\n" +
				"\n" +
				"</head>\n" +
				"\n" +
				"<body style=\"margin:0; padding:20px; background-color:#f0f0f0;\">\n" +
				"\n" +
				"<center>\n" +
				"<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"100%%\" style=\"max-width:600px; margin:0 auto;\">\n" +
				"<tr>\n" +
				"<td style=\"padding:0;\">\n" +
				"\n" +
				"<div class=\"card\">\n" +
				"\n" +
				"<div class=\"welcome-title\">\n" +
				"Hola %s\n" +
				"</div>\n" +
				"\n" +
				"<div class=\"instruction\">\n" +
				"Para cambiar su contraseña, introduce el siguiente código:\n" +
				"</div>\n" +
				"\n" +
				"<div class=\"code\">\n" +
				"%s\n" +
				"</div>\n" +
				"\n" +
				"<div class=\"instruction\">\n" +
				"El código expirará en 10 minutos\n" +
				"</div>\n" +
				"\n" +
				"</div>\n" +
				"\n" +
				"</td>\n" +
				"</tr>\n" +
				"</table>\n" +
				"</center>\n" +
				"\n" +
				"</body>\n" +
				"</html>",
				username, verCode
				);
		try
		{
			var mimeMessage = javaMailSender.createMimeMessage();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage,MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,"UTF-8");
			
			helper.setFrom(this.sender,"Campoaras");
			helper.setTo(receiver);
			helper.setSubject(subject);
			helper.setText(htmlContent,true);
			
			this.javaMailSender.send(mimeMessage);
			
			log.info("[ADMIN] -- /register -- Correo de verificacion de registro enviado con exito a {} -- {}",usrToken,seguridad);
		}
		catch(MailException | MessagingException |  UnsupportedEncodingException exception)
		{
			log.error("[ERROR] No se ha podido enviar el mensaje de correo a {} - {} - causa {}",usrToken,seguridad,exception);
			throw new CPException(500,"Error interno del servidor, problemas al enviar mail",exception);
		}
	}
}
