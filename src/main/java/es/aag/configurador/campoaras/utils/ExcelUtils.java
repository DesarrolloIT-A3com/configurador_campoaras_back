package es.aag.configurador.campoaras.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.services.EncryptorService;

public class ExcelUtils 
{
	
	
	public CellStyle crearEstiloTitulo(XSSFWorkbook workbook)
	{
		Font fuente = workbook.createFont();
		fuente.setBold(true);
		fuente.setColor(IndexedColors.WHITE.getIndex());
 
		CellStyle estilo = workbook.createCellStyle();
		estilo.setFont(fuente);
		estilo.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
		estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return estilo;
	}
	
	public CellStyle crearEstiloCabecera(XSSFWorkbook workbook)
	{
		Font fuente = workbook.createFont();
		fuente.setBold(true);
 
		CellStyle estilo = workbook.createCellStyle();
		estilo.setFont(fuente);
		estilo.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		return estilo;
	}
	
	public Row createCabecera(Sheet sheet,CellStyle estiloCabecera,int numFila,Configuracion config,EncryptorService encryptor)
	{
		Row row = sheet.createRow(numFila);
		
		Cell celda = row.createCell(0);
		celda.setCellValue("referencia");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(1);
		celda.setCellValue("fondo");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(2);
		celda.setCellValue("ancho");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(3);
		celda.setCellValue("alto");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(4);
		celda.setCellValue("alto max");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(5);
		celda.setCellValue("fondo min");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(6);
		celda.setCellValue("fondo max");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(7);
		celda.setCellValue("fondo especial");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(8);
		celda.setCellValue("ancho especial");
		celda.setCellStyle(estiloCabecera);
		
		celda = row.createCell(9);
		celda.setCellValue("alto especial");
		celda.setCellStyle(estiloCabecera);
		
		int column = 10;
		
		for(Map<String,Object> item:config.getArmazon())
		{
			celda = row.createCell(column);
			String nombre = null;
			if(encryptor.decrypt((String) item.get("nombre"))==null)
			{
				nombre = "";
			}
			else
			{
				nombre = encryptor.decrypt((String) item.get("nombre"));
				
			}
			celda.setCellValue(nombre);
			celda.setCellStyle(estiloCabecera);
			column++;
		}
		
		return row;
		
	}
	
	public Row createDatos(Sheet sheet,int numFila,Configuracion config,EncryptorService encryptor)
	{
		Row row = sheet.createRow(numFila);
		
		Cell celda = row.createCell(0);
		celda.setCellValue(config.getReferencia());
		
		celda = row.createCell(1);
		celda.setCellValue(config.getFondo());
		
		celda = row.createCell(2);
		celda.setCellValue(config.getAncho());
		
		celda = row.createCell(3);
		celda.setCellValue(config.getAlto());
		
		celda = row.createCell(4);
		celda.setCellValue(config.getAltoMax());
		
		celda = row.createCell(5);
		celda.setCellValue(config.getFondoMin());
		
		celda = row.createCell(6);
		celda.setCellValue(config.getFondoMax());
		
		celda = row.createCell(7);
		celda.setCellValue(config.getPrecioMedidaFondoEsp());
		
		celda = row.createCell(8);
		celda.setCellValue(config.getPrecioMedidaAnchoEsp());
		
		celda = row.createCell(9);
		celda.setCellValue(config.getPrecioMedidaAltoEsp());
		
		int column = 10;
		
		for(Map<String,Object> item:config.getArmazon())
		{
			celda = row.createCell(column);
			Number precioRaw = (Number) item.get("precio");
			float precio = precioRaw != null ? precioRaw.floatValue() : 0;
			celda.setCellValue(precio);
			column++; 
		}
		
		return row;
	}
	
	public Row crearTituloFusionado(Sheet sheet, CellStyle estiloTitulo, int numFila, int ultimaColumna, String texto)
	{
	    Row row = sheet.createRow(numFila);
	    Cell celda = row.createCell(0);
	    celda.setCellValue(texto);
	    celda.setCellStyle(estiloTitulo);

	    for (int c = 1; c <= ultimaColumna; c++)
	    {
	        Cell cAux = row.createCell(c);
	        cAux.setCellStyle(estiloTitulo);
	    }

	    sheet.addMergedRegion(new CellRangeAddress(numFila, numFila, 0, ultimaColumna));

	    return row;
	}
	
	public boolean validateHeader(Configuracion config,List<String> columnas,EncryptorService encryptor)
	{
		String [] staticHeaders = new String [] {"referencia","fondo","ancho","alto","alto max","fondo min","fondo max","fondo especial","ancho especial","alto especial"};
		
		
		boolean isValid = columnas.size() > staticHeaders.length;
		
		if(isValid)
		{
			for(int i = 0;i<staticHeaders.length;i++)
			{
				if(isValid)
				{
					isValid = staticHeaders[i].equals(columnas.get(i));
				}
			}
		}
		
		return isValid;
	}
	
	public List<String> extractVariableHeaders(List<String> columnas)
	{
		List<String> result = new ArrayList<String>();
		List<String> staticHeaders = List.of("referencia","fondo","ancho","alto","alto max","fondo min","fondo max","fondo especial","ancho especial","alto especial");
		
		for(String value:columnas)
		{
			if(!staticHeaders.contains(value))
			{
				result.add(value);
			}
		}
		
		return result;
	}
	
	public boolean isRowBlank(Row fila)
	{		
		if(fila==null)
		{
			return true;
		}
		
		Cell celda = fila.getCell(0);
		if (celda == null || celda.getCellType() != CellType.STRING || "".equals(celda.getStringCellValue()))
		{
			return true;
		}
		
		for (int c = 1; c < fila.getLastCellNum(); c++)
		{
			celda = fila.getCell(c);
			if (celda != null && celda.getCellType() == CellType.BLANK)
			{
				return true;
			}
		}
		return false;
	}
	
	public float redondearColumna(String valor) 
	{
	    BigDecimal bd = new BigDecimal(valor.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
	    return bd.floatValue();
	}
}
