package es.aag.configurador.campoaras.services;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.entities.Acabado;
import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IAcabadoRepository;
import es.aag.configurador.campoaras.repositories.IBulkProductosUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IColorRepository;
import es.aag.configurador.campoaras.repositories.IProductoConfiguradoRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.utils.CPException;
import es.aag.configurador.campoaras.utils.Validations;

@Service
public class OrderService 
{
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private EncryptorService encryptor;
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IAcabadoRepository acabadoRepo;
	
	@Autowired
	private IColorRepository colorRepo;
	
	@Autowired
	private IProductoConfiguradoRepository seleccionRepo;
	
	@Autowired
	private IBulkProductosUsuarioRepository bulkRepo;
	
	private final Validations validation;
	
	public OrderService()
	{
		this.validation = new Validations();
	}
	
	public List<ResponseSeleccion> getSelecciones(Boolean isEnd,String userUuid,String rol,String seguridad,String usrToken)
	{
		List<ResponseSeleccion> response = new LinkedList<ResponseSeleccion>();
		
		LocalDateTime fecha = null;
		
		Set<BulkProductosUsuario> bulkList = null;
		
		if(userUuid!=null)
		{
			Optional<Usuario> optUser = this.userRepo.findById(userUuid);
			
			if(optUser.isPresent())
			{
				bulkList = optUser.get().getSelecciones();
			}
		}
		
		if(bulkList==null)
		{
			bulkList = new HashSet<BulkProductosUsuario>(this.bulkRepo.findAll());
		}
		
		String usuario = "";
		
		for(BulkProductosUsuario bulk:bulkList)
		{
			if(isEnd!=null)
			{
				if(bulk.isEnd() != isEnd)
				{
					continue;
				}
			}
			
			
			SeleccionDTO [] selecciones = new SeleccionDTO[bulk.getProductos().size()];
			int index = 0;
			
			for(String producto:bulk.getProductos())
			{
				Optional<ProductoConfigurado> seleccionOpt = this.seleccionRepo.findById(producto);
				if(seleccionOpt.isPresent())
				{
					ProductoConfigurado item = seleccionOpt.get();
					
					String uuid = item.getUuid();
					usuario = this.encryptor.decrypt(item.getUsuario().getUsername());
					String referencia = item.getConfiguracion().getReferencia();
					String armazon = this.encryptor.decrypt(item.getAcabado().getNombre());
					String colorArmazon = this.encryptor.decrypt(item.getColorArmazon().getNombre());
					String acabadoFrente = this.encryptor.decrypt(item.getAcabadoFrente().getNombre());
					String colorFrente = this.encryptor.decrypt(item.getColorFrente().getNombre());
					String frente = this.encryptor.decrypt(item.getFrente().getNombre());
					String acabadoTirador = null;
					String acabadoRegleta = null;
					String colorTirador = null;
					String colorRegleta = null;
					
					if(item.getAcabadoTirador()!=null)
					{
						acabadoTirador = this.encryptor.decrypt(item.getAcabadoTirador().getNombre());
						colorTirador = this.encryptor.decrypt(item.getColorTirador().getNombre());

					}
					
					if(item.getAcabadoRegleta() != null)
					{
						acabadoRegleta = this.encryptor.decrypt(item.getAcabadoRegleta().getNombre());
						colorRegleta = this.encryptor.decrypt(item.getColorRegleta().getNombre());

				    }
					
					Float precioArmazon = item.getPrecioArmazon();
					Float precioFrente = item.getPrecioFrente();
					Float precioTirador = item.getPrecioTirador();
					Float precioRegleta = item.getPrecioRegleta();
					Float precioFinal = item.getPrecioFinal();
					
					int cantidad = item.getCantidad();
					
					// Estos ternarios asignan el valor las medidas del producto configurado que serían las medidas especiales, en caso de ser nulas, se asignan la de la referencia escogida
					float fondo = item.getFondo() != null ? item.getFondo() : item.getConfiguracion().getFondo();
					float ancho = item.getAncho() != null ? item.getAncho() : item.getConfiguracion().getAncho();
					float alto = item.getAlto() != null ? item.getAlto() : item.getConfiguracion().getAlto();
					
					String serie = this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getNombre());
					serie += " "+this.encryptor.decrypt(item.getConfiguracion().getSerie().getVariante());
					
					SeleccionDTO seleccion = new SeleccionDTO(uuid, referencia, null,serie,fondo,ancho,alto, precioArmazon, armazon, colorArmazon,precioFrente, frente, acabadoFrente, colorFrente,precioTirador, acabadoTirador, colorTirador,precioRegleta, acabadoRegleta, colorRegleta,precioFinal, cantidad,null);
					selecciones[index] = seleccion;
					
					if(fecha==null)
					{
						fecha = item.getFecha();
					}
					else if(fecha.isAfter(item.getFecha()))
					{
						fecha = item.getFecha();
					}
				}
				else
				{
					selecciones[index] = null;
				}
				
				index++;
				
			}
			ResponseSeleccion seleccion = new ResponseSeleccion(bulk.getUuid(),usuario,selecciones,fecha,bulk.isEnd());
			
			response.add(seleccion);
		}
		
		return response;
	}
	
	public void deleteSeleccion(String uuid,String rol,String seguridad,String usrToken) throws CPException
	{
		Optional<ProductoConfigurado> seleccionOpt = this.seleccionRepo.findById(uuid);
		
		if(!seleccionOpt.isPresent())
		{
			log.warn("[AVISO] -- /producto-configurado -- {} Ha solicitado una seleccion que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Dato o datos no encontrados");
		}
		
		ProductoConfigurado seleccion = seleccionOpt.get();
		
		log.info("[ADMIN] -- /producto-configurado -- {} Ha eliminado la seleccion {} de la base de datos con permiso de {} -- {}",usrToken,seleccion.getUuid(),rol,seguridad);
		this.seleccionRepo.delete(seleccion);
		List<BulkProductosUsuario> bulkList = this.bulkRepo.findAll();
		
		for(BulkProductosUsuario bulk:bulkList)
		{
			int index = bulk.getProductos().indexOf(seleccion.getUuid());
			
			if(index !=-1)
			{
				List<String> productos = bulk.getProductos();
				productos.remove(index);
				bulk.setProductos(productos);
				this.bulkRepo.save(bulk);
				this.bulkRepo.flush();
				break;
			}
		}
		
		
	}
 	
}
