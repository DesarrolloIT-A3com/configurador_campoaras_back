package es.aag.configurador.campoaras.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import es.aag.configurador.campoaras.dto.ResponseAcabado;
import es.aag.configurador.campoaras.dto.ResponseColor;
import es.aag.configurador.campoaras.dto.ResponseConfiguracion;
import es.aag.configurador.campoaras.dto.ResponseFrente;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.entities.Acabado;
import es.aag.configurador.campoaras.entities.Color;
import es.aag.configurador.campoaras.entities.Frente;
import es.aag.configurador.campoaras.entities.Producto;
import es.aag.configurador.campoaras.entities.Serie;
import es.aag.configurador.campoaras.repositories.IAcabadoRepository;
import es.aag.configurador.campoaras.repositories.IColorRepository;
import es.aag.configurador.campoaras.repositories.IConfiguracionRepository;
import es.aag.configurador.campoaras.repositories.IFrenteRepository;
import es.aag.configurador.campoaras.repositories.IProductoRepository;
import es.aag.configurador.campoaras.repositories.ISerieRepository;
import es.aag.configurador.campoaras.services.EncryptorService;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
public class Validations 
{
	
	private IProductoRepository productoRepo;
	
	private IFrenteRepository frenteRepo;
	
	private IAcabadoRepository acabadoRepo;
	
	private IColorRepository colorRepo;
	
	private ISerieRepository seriesRepo;
	
	private IConfiguracionRepository configuracionRepo;
	
	private EncryptorService encryptor;
	

	/**
	 * Metodo que inicializa uno o todos repositorios que el servicio ManagmentService decida para el uso de las validaciones 
	 * @param productoRepo
	 * @param frenteRepo
	 * @param acabadoRepo
	 * @param colorRepo
	 * @param seriesRepo
	 * @param configuracionRepo
	 * @param encryptor
	 */
	public void initialize(IProductoRepository productoRepo, IFrenteRepository frenteRepo, IAcabadoRepository acabadoRepo,
			IColorRepository colorRepo, ISerieRepository seriesRepo, IConfiguracionRepository configuracionRepo,
			EncryptorService encryptor) 
	{
		this.productoRepo = productoRepo;
		this.frenteRepo = frenteRepo;
		this.acabadoRepo = acabadoRepo;
		this.colorRepo = colorRepo;
		this.seriesRepo = seriesRepo;
		this.configuracionRepo = configuracionRepo;
		this.encryptor = encryptor;
	}
	
	/**
	 * Metodo que destruye todas las instancias de los repositorios para que no se usen inadecuadamente
	 */
	public void destroy()
	{
		this.productoRepo = null;
		this.frenteRepo = null;
		this.acabadoRepo = null;
		this.colorRepo = null;
		this.seriesRepo = null;
		this.configuracionRepo = null;
		this.encryptor = null;
	}
	
	public boolean validateProduct(Producto producto)
	{
		boolean validate = true;
		
		validate = producto.getNombre() != null && producto.getTipo() != null;
		
		if(validate)
		{
			validate = !producto.getNombre().isBlank() && !producto.getTipo().isBlank();
		}
			
		return validate;
	}
	
	public boolean validateSerie(Serie serie)
	{
		boolean validate = true;
		
		validate = serie.getVariante() != null && serie.getModulo() != null;
		
		if(validate)
		{
			validate = !serie.getVariante().isBlank() && !serie.getModulo().isBlank();
		}
			
		return validate;
	}
	
	public boolean validateAcabado(ResponseAcabado acabado)
	{
		boolean validate = true;
		
		validate = acabado.getNombre() != null;
		
		if(validate)
		{
			validate = !acabado.getNombre().isBlank();
		}
			
		return validate;
	}
	
	public boolean validateColor(ResponseColor color)
	{
		boolean validate = true;
		
		validate = color.getNombre() != null;
		
		if(validate)
		{
			validate = !color.getNombre().isBlank();
		}
			
		return validate;
	}
	
	public boolean validateFrente(ResponseFrente frente)
	{
		boolean validate = true;
		
		validate = frente.getNombre() != null && frente.getReferencia() != null;
		
		if(validate)
		{
			validate = !frente.getNombre().isBlank() && !frente.getReferencia().isBlank();
		}
		
		return validate;
	}
	
	public boolean validateConfiguration(ResponseConfiguracion configuracion)
	{
		boolean validate = true;
		
		validate = configuracion.getFondo() >= 0 && configuracion.getAncho() >= 0 && configuracion.getAlto() >= 0;
		
		if(validate)
		{
			validate = configuracion.getArmazon() != null;
		}
		
		if(validate)
		{
			validate = configuracion.getArmazon().size()>0;
		}
		
		return validate;		
	}
	
	public boolean validateSeleccion(SeleccionDTO seleccion)
	{
		boolean validate = true;
		
		validate = seleccion.getArmazon() !=null && seleccion.getAcabadoFrente() !=null && seleccion.getFrente() != null && seleccion.getReferencia() != null;
		
		if(validate)
		{
			validate = !seleccion.getArmazon().isBlank() && !seleccion.getAcabadoFrente().isBlank() && !seleccion.getFrente().isBlank() && seleccion.getCantidad() > 0 && !seleccion.getReferencia().isBlank();
		}
		
		return validate;
	}
	
	
	public Producto findProducts(String nombre)
	{
		Producto found = null;
		List<Producto> productos = this.productoRepo.findAll();
		
		int index = 0;
		
		while(index < productos.size() && found==null)
		{
			Producto producto = productos.get(index);
			
			if(this.encryptor.decrypt(producto.getNombre()).equals(nombre))
			{
				found = producto;
			}
			
			index++;
		}
		
		return found;
	}
	
	public Serie findSerie(String variante)
	{
		Serie found = null;
		List<Serie> series = this.seriesRepo.findAll();
		
		int index = 0;
		
		while(index < series.size() && found==null)
		{
			Serie serie = series.get(index);
			
			if(this.encryptor.decrypt(serie.getVariante()).equals(variante))
			{
				found = serie;
			}
			
			index++;
		}
		
		return found;
	} 
	
	public Acabado findAcabado(String nombre)
	{
		Acabado found = null;
		List<Acabado> acabados = this.acabadoRepo.findAll();
		
		int index = 0;
		
		while(index < acabados.size() && found==null)
		{
			Acabado acabado = acabados.get(index);
			
			if(this.encryptor.decrypt(acabado.getNombre()).equals(nombre))
			{
				found = acabado;
			}
			
			index++;
		}
		
		return found;
	}
	
	public Color findColor(String nombre)
	{
		Color found = null;
		List<Color> colores = this.colorRepo.findAll();
		
		int index = 0;
		
		while(index < colores.size() && found==null)
		{
			Color color = colores.get(index);
			
			if(this.encryptor.decrypt(color.getNombre()).equals(nombre))
			{
				found = color;
			}
			
			index++;
		}
		
		return found;
	}
	
	public Frente findFrentes(String nombre)
	{
		Frente found = null;
		List<Frente> frentes = this.frenteRepo.findAll();
		
		int index = 0;
		
		while(index < frentes.size() && found==null)
		{
			Frente frente = frentes.get(index);
			
			if(this.encryptor.decrypt(frente.getNombre()).equals(nombre))
			{
				found = frente;
			}
			
			index++;
		}
		
		return found;
	}
	
	public List<Map<String,Object>> sanearArmazon(List<Map<String,Object>> armazon)
	{
		List<Map<String,Object>> armazonSaneado = new LinkedList<Map<String,Object>>();
		
		for(Map<String,Object> item:armazon)
		{
			if(item.keySet().size()==2)
			{
				String nombreAcabado = (String) item.getOrDefault("nombre", CPConstants.MAP_DEFAULT_VALUE);
				
				if( nombreAcabado != null)
				{
					if(!nombreAcabado.equals(CPConstants.MAP_DEFAULT_VALUE) && this.findAcabado(nombreAcabado)!=null)
					{						
						
						item.put("nombre", this.encryptor.encrypt(nombreAcabado));
						Float precio = null;
						Number precioNum = (Number) item.getOrDefault("precio", -1);
						
						if(precioNum!=null)
						{
							precio = precioNum.floatValue();
						}
						
						if(precio != null)
						{
							if(precio>=0)
							{
								armazonSaneado.add(item);
							}
						}
					}
				}
			}
		}
		return armazonSaneado;
	}
	
	public List<Map<String,Object>> sanearExtras(List<Map<String,Object>> extras)
	{
		List<Map<String,Object>> extrasSaneado = new LinkedList<Map<String,Object>>();
		
		if(extras!=null)
		{
			for(Map<String,Object> item:extras)
			{
				if(item.keySet().size()==2)
				{
					String nombreExtra = (String) item.getOrDefault("nombre", CPConstants.MAP_DEFAULT_VALUE);
					Number precioNum = (Number) item.getOrDefault("precio", -1);
					Float precio = null;
					
					if(precioNum!=null)
					{
						precio = precioNum.floatValue();
					}
					
					if(nombreExtra!=null && precio!=null)
					{
						if(!nombreExtra.equals(CPConstants.MAP_DEFAULT_VALUE) && precio>=0)
						{
							item.put("nombre", this.encryptor.encrypt(nombreExtra));
							extrasSaneado.add(item);
						}
					}
				}
			}
		}
		
		return extrasSaneado;
	}

}
