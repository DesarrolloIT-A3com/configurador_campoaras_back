package es.aag.configurador.campoaras.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.aag.configurador.campoaras.dto.OrderDTO;
import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.Frente;
import es.aag.configurador.campoaras.entities.Pedido;
import es.aag.configurador.campoaras.entities.PedidoBackup;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;
import es.aag.configurador.campoaras.entities.Serie;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IBulkProductosUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IConfiguracionRepository;
import es.aag.configurador.campoaras.repositories.IPedidoBackupRepository;
import es.aag.configurador.campoaras.repositories.IPedidoRepository;
import es.aag.configurador.campoaras.repositories.IProductoConfiguradoRepository;
import es.aag.configurador.campoaras.repositories.ISerieRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import es.aag.configurador.campoaras.utils.EstadoPedido;

@Service
public class OrderService 
{
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private EncryptorService encryptor;
	
	@Autowired
	private MailService mailService;
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IProductoConfiguradoRepository seleccionRepo;
	
	@Autowired
	private IBulkProductosUsuarioRepository bulkRepo;
	
	@Autowired
	private IPedidoRepository pedidoRepo;
	
	@Autowired
	private IPedidoBackupRepository pedidoBakRepo;
	
	@Autowired
	private ISerieRepository serieRepo;
	
	@Autowired
	private IConfiguracionRepository configRepo;
	
	
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
			String referenciaBulk = this.encryptor.decrypt(bulk.getReferencia());
			
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
					String colorArmazon = "Sin color";
					if(item.getCodigoColorArmazon()==null)
					{
						colorArmazon = this.encryptor.decrypt(item.getColorArmazon().getNombre());
					}
					else
					{
						colorArmazon = this.encryptor.decrypt(item.getCodigoColorArmazon());
					}
					
					String acabadoFrente = "-";
					String colorFrente = "-";
					String frente = "-";
					if(item.getFrente()!=null)
					{
						acabadoFrente = this.encryptor.decrypt(item.getAcabadoFrente().getNombre());
						if(item.getColorFrente()!=null)
						{
							colorFrente = this.encryptor.decrypt(item.getColorFrente().getNombre());
						}
						else
						{
							colorFrente = this.encryptor.decrypt(item.getCodigoColorFrente());
						}
						frente = this.encryptor.decrypt(item.getFrente().getNombre());
					}
					
					String acabadoTirador = null;
					String acabadoRegleta = null;
					String colorTirador = null;
					String colorRegleta = null;
					
					if(item.getAcabadoTirador()!=null)
					{
						acabadoTirador = this.encryptor.decrypt(item.getAcabadoTirador().getNombre());
						if(item.getColorTirador()!=null)
						{
							colorTirador = this.encryptor.decrypt(item.getColorTirador().getNombre());
						}
						else
						{
							colorTirador = this.encryptor.decrypt(item.getCodigoColorTirador());
						}
					}
					
					if(item.getAcabadoRegleta() != null)
					{
						acabadoRegleta = this.encryptor.decrypt(item.getAcabadoRegleta().getNombre());
						if(item.getColorRegleta()!=null)
						{
							colorRegleta = this.encryptor.decrypt(item.getColorRegleta().getNombre());
						}
						else
						{
							colorRegleta = this.encryptor.decrypt(item.getCodigoColorTirador());
						}
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
					
					// Si existen medidas especiales se marca que la configuración presenta la etiqueta ESP
					boolean isEspecial = fondo!=item.getConfiguracion().getFondo() || ancho!=item.getConfiguracion().getAncho() || alto!=item.getConfiguracion().getAlto();
					
					String serie = this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getNombre());
					serie += " "+this.encryptor.decrypt(item.getConfiguracion().getSerie().getVariante());
					
					String tipo = this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getTipo());
					
					List<String> extrasDecrypt = new LinkedList<String>();
					
					for(String extra:item.getExtras())
					{
						extrasDecrypt.add(this.encryptor.decrypt(extra));
					}
					
					String observaciones = "";
					
					if(item.getObservaciones()!=null)
					{
						observaciones = this.encryptor.decrypt(item.getObservaciones());
					}
					
					SeleccionDTO seleccion = new SeleccionDTO(uuid, referencia, null,serie,fondo,ancho,alto, precioArmazon, armazon, colorArmazon,precioFrente, frente, acabadoFrente, colorFrente,precioTirador, acabadoTirador, colorTirador,precioRegleta, acabadoRegleta, colorRegleta, extrasDecrypt,precioFinal, cantidad, observaciones,isEspecial,tipo,null,null);
					selecciones[index] = seleccion;
				}
				else
				{
					selecciones[index] = null;
				}
				
				index++;
				
			}
			if(bulk.getFecha()==null)
			{
				fecha=LocalDateTime.now();
			}
			else
			{
				fecha=bulk.getFecha();
			}
			ResponseSeleccion seleccion = new ResponseSeleccion(bulk.getUuid(),usuario,referenciaBulk,selecciones,fecha,bulk.isEnd());
			
			response.add(seleccion);
		}
		
		log.info("[ACCION] -- /producto-configurado -- {} Ha solicitado un listado de selecciones con un permiso de {} -- {}",usrToken,rol,seguridad);
		
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
		
		List<BulkProductosUsuario> bulkList = this.bulkRepo.findAll();
		
		boolean isDelete = false;
		BulkProductosUsuario bulk = null;
		
		for(BulkProductosUsuario item:bulkList)
		{
			int index = item.getProductos().indexOf(seleccion.getUuid());
			
			if(index !=-1)
			{
				List<String> productos = item.getProductos();
				productos.remove(index);
				item.setProductos(productos);
				if(item.getProductos().isEmpty())
				{
					this.bulkRepo.delete(item);
					isDelete = true;
				}
				else
				{
					this.bulkRepo.save(item);
					bulk = item;
				}
				this.bulkRepo.flush();
				break;
			}
		}
		
		// Se buscan los lavabos para cambiarlos de tipología en caso de que que dentro de una cesta no haya ningún tipo de mueble
		if(!isDelete)
		{
			List<ProductoConfigurado> lavabos = new LinkedList<ProductoConfigurado>();
			boolean hayMueble = false;
			
			for(String producto:bulk.getProductos())
			{
				Optional<ProductoConfigurado> optItem = this.seleccionRepo.findById(producto);
				
				if(optItem.isPresent())
				{
					ProductoConfigurado item = optItem.get();
					
					if(this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getTipo()).equalsIgnoreCase("lavabos"))
					{
						lavabos.add(item);
					}
					
					if(this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getTipo()).equalsIgnoreCase("mueble"))
					{
						hayMueble = true;
					}
				}
			}
			
			if(!hayMueble && lavabos.size()>0)
			{
				for(ProductoConfigurado item:lavabos)
				{
					Serie serieSelect = null;
					List<Serie> variantesLavabo = this.serieRepo.findByProducto(item.getConfiguracion().getSerie().getProducto());

					
					for(Serie serie:variantesLavabo)
					{
						String variante = this.encryptor.decrypt(serie.getVariante()).strip();
						String configVariante = this.encryptor.decrypt(item.getConfiguracion().getSerie().getVariante()).strip();
						String [] spliter =  variante.split(configVariante);
						
						if(spliter.length == 2 && spliter[1].strip().equalsIgnoreCase("solo"))
						{
							serieSelect = serie;
							break;
						}
					}
					
					if(serieSelect!=null)
					{
						List<Configuracion> configs = this.configRepo.findBySerie(serieSelect);
						String referencia = item.getConfiguracion().getReferencia().replace("-C", "");
						
						boolean isChanged = false;
						
						for(Configuracion config:configs)
						{
							if(config.getReferencia().equalsIgnoreCase(referencia))
							{
								item.setConfiguracion(config);
								
								String acabado = this.encryptor.decrypt(item.getAcabado().getNombre());		
								
								for(Map<String,Object> armazones:config.getArmazon())
								{
									String armazon = this.encryptor.decrypt(((String) armazones.get("nombre")));
									
									if(acabado.equalsIgnoreCase(armazon))
									{
										Number precio = (Number) armazones.get("precio");
										item.setPrecioFinal(precio.floatValue() * item.getCantidad());
										isChanged = true;
										break;
									}
								}
							}
						}
						
						log.info("[ACCION] -- /producto-configurado -- {} Se ha alterado los lavabos de la cesta {} debido a que no hay muebles con permiso de {} -- {}",usrToken,bulk.getUuid(),rol,seguridad);
						
						this.seleccionRepo.save(item);
						this.seleccionRepo.flush();
					}
					
				}
			}
		}
		
		List<Pedido> allPedidos = this.pedidoRepo.findAll();
		Set<Pedido> pedidos = new HashSet<Pedido>();
		
		for(Pedido pedido:allPedidos)
		{
			if(pedido.getProductos().contains(seleccion.getUuid()))
			{
				pedidos.add(pedido);
				break;
			}
		}
		
		this.pedidoRepo.deleteAll(pedidos);
		this.pedidoRepo.flush();
		
		log.info("[ACCION] -- /producto-configurado -- {} Ha eliminado la seleccion {} de la base de datos con permiso de {} -- {}",usrToken,seleccion.getUuid(),rol,seguridad);
		this.seleccionRepo.delete(seleccion);
		this.seleccionRepo.flush();
		
		
	}
	
	public void postOrder(OrderDTO body,String rol,String seguridad,String usrToken) throws CPException
	{
		Optional<Usuario> optUser = this.userRepo.findById(body.getUsuario());

		if(!optUser.isPresent())
		{
			log.warn("[AVISO] -- /order-proposal -- {} Ha intentado tramitar un pedido introduciendo un uuid de usuario erroneo con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Datos inexistentes");
		}
		
		Usuario usuario = optUser.get();
		
		if(body.getReferencia()==null || body.getProductos()==null)
		{
			log.warn("[AVISO] -- /order-proposal -- {} Ha intentado tramitar un pedido con datos nulos con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		if(!body.getReferencia().isBlank() && body.getProductos().length==0)
		{
			log.warn("[AVISO] -- /order-proposal -- {} Ha intentado tramitar un pedido con datos vacios con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		Pedido pedido = new Pedido();
		List<String> productoList = new LinkedList<String>();
		List<ProductoConfigurado> selecciones = new LinkedList<ProductoConfigurado>();
		  
		for(String producto:body.getProductos())
		{
			Optional<ProductoConfigurado> optSeleccion = this.seleccionRepo.findById(producto);
			
			if(!optSeleccion.isPresent())
			{
				log.warn("[AVISO] -- /order-proposal -- {} Ha intentado tramitar un pedido introduciendo un uuid de seleccion erroneo con permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(404,"Datos inexistentes");
			}
			
			productoList.add(producto);
			selecciones.add(optSeleccion.get());
		}
		
		String uuid = UUID.randomUUID().toString();
		
		pedido.setUuid(uuid);
		pedido.setProductos(productoList);
		pedido.setReferencia(this.encryptor.encrypt(body.getReferencia()));
		pedido.setUsuarioPedido(usuario);
		pedido.setFecha(LocalDateTime.now());
		pedido.setEstado(EstadoPedido.CURSADO);
		
		log.info("[ACCION] -- /order-proposal -- {} Ha tramitado un pedido con permiso de {} -- {}",usrToken,rol,seguridad);
		
		this.pedidoRepo.save(pedido);
		this.pedidoRepo.flush();
		usuario.addPedidos(pedido);
		this.userRepo.save(usuario);
		this.userRepo.flush();
		
		this.registerPedidoBak(usuario, pedido, selecciones,rol, seguridad, usrToken);
	}
	
	public List<OrderDTO> getPedidos(Usuario usuario,String rol,String seguridad,String usrToken)
	{
		List<OrderDTO> response = new LinkedList<OrderDTO>();
		
		for(Pedido pedido:usuario.getPedidos())
		{
			String uuid = pedido.getUuid();
			String referencia = this.encryptor.decrypt(pedido.getReferencia());
			EstadoPedido estado = pedido.getEstado();
			List<SeleccionDTO> selecciones = new LinkedList<SeleccionDTO>();
			
			for(String producto:pedido.getProductos())
			{
				Optional<ProductoConfigurado> optSeleccion = this.seleccionRepo.findById(producto);
				
				if(optSeleccion.isPresent())
				{
					ProductoConfigurado seleccion = optSeleccion.get();
					String uuidSel = seleccion.getUuid();
					String referenciaSel = seleccion.getConfiguracion().getReferencia();		
					if(seleccion.getFrente()!=null)
					{
						referenciaSel = this.encryptor.decrypt(seleccion.getFrente().getReferencia()) +" "+referenciaSel;
					}
					String armazon = this.encryptor.decrypt(seleccion.getAcabado().getNombre());
					String colorArmazon = "Sin color";
					if(seleccion.getCodigoColorArmazon()==null)
					{
						colorArmazon = this.encryptor.decrypt(seleccion.getColorArmazon().getNombre());
					}
					else
					{
						colorArmazon = this.encryptor.decrypt(seleccion.getCodigoColorArmazon());
					}
					
					String frente = "-";
					String acabadoFrente = "-";
					String colorFrente = "-";
					
					if(seleccion.getFrente()!=null)
					{
						acabadoFrente = this.encryptor.decrypt(seleccion.getAcabadoFrente().getNombre());
						if(seleccion.getColorFrente()!=null)
						{
							colorFrente = this.encryptor.decrypt(seleccion.getColorFrente().getNombre());
						}
						else
						{
							colorFrente = this.encryptor.decrypt(seleccion.getCodigoColorFrente());
						}
						frente = this.encryptor.decrypt(seleccion.getFrente().getNombre());
					}
					
					String acabadoRegleta = null;
					String acabadoTirador = null;
					String colorRegleta = null;
					String colorTirador = null;
					
					if(seleccion.getAcabadoRegleta()!=null)
					{
						acabadoRegleta = this.encryptor.decrypt(seleccion.getAcabadoRegleta().getNombre());
						if(seleccion.getColorRegleta()!=null)
						{
							colorRegleta = this.encryptor.decrypt(seleccion.getColorRegleta().getNombre());
						}
						else
						{
							colorRegleta = this.encryptor.decrypt(seleccion.getCodigoColorTirador());
						}
					}
					
					if(seleccion.getAcabadoTirador()!=null)
					{
						acabadoTirador = this.encryptor.decrypt(seleccion.getAcabadoTirador().getNombre());
						if(seleccion.getColorTirador()!=null)
						{
							colorTirador = this.encryptor.decrypt(seleccion.getColorTirador().getNombre());
						}
						else
						{
							colorTirador = this.encryptor.decrypt(seleccion.getCodigoColorTirador());
						}
					}
					
					// En caso de que no se hayan seleccionado medidas especiales se cogen las medidas base
					float fondo = seleccion.getFondo()!=null ? seleccion.getFondo() : seleccion.getConfiguracion().getFondo();
					float ancho = seleccion.getAncho()!=null ? seleccion.getAncho() : seleccion.getConfiguracion().getAncho();
					float alto = seleccion.getAlto()!=null ? seleccion.getAlto() : seleccion.getConfiguracion().getAlto();
					
					// Si existen medidas especiales se marca que la configuración presenta la etiqueta ESP
					boolean isEspecial = fondo!=seleccion.getConfiguracion().getFondo() || ancho!=seleccion.getConfiguracion().getAncho() || alto!=seleccion.getConfiguracion().getAlto();
					
					// Conversión a milimetros
					fondo = fondo * 10;
					ancho = ancho * 10;
					alto = alto * 10;
					
					float precioFinal = seleccion.getPrecioFinal();
					int cantidad = seleccion.getCantidad();
					
					List<String> extrasDecrypt = new LinkedList<String>();
					
					for(String extra:seleccion.getExtras())
					{
						extrasDecrypt.add(this.encryptor.decrypt(extra));
					}
					
					String serie = this.encryptor.decrypt(seleccion.getConfiguracion().getSerie().getProducto().getNombre()) +" "+this.encryptor.decrypt(seleccion.getConfiguracion().getSerie().getVariante());
					
					String tipo = this.encryptor.decrypt(seleccion.getConfiguracion().getSerie().getProducto().getTipo());
					
					String observaciones = "";
					
					if(seleccion.getObservaciones()!=null)
					{
						observaciones = this.encryptor.decrypt(seleccion.getObservaciones());
					}
					
					SeleccionDTO dto = new SeleccionDTO(uuidSel,referenciaSel,this.encryptor.decrypt(usuario.getUsername()),serie,fondo,ancho,alto,null,armazon,colorArmazon,null,frente,acabadoFrente,
							colorFrente,null,acabadoTirador,colorTirador,null,acabadoRegleta,colorRegleta,extrasDecrypt,precioFinal,cantidad,observaciones,isEspecial,tipo,null,null);
					
					selecciones.add(dto);
				}
			}
			response.add(new OrderDTO(uuid,referencia,this.encryptor.decrypt(usuario.getUsername()),pedido.getFecha(),null,estado, selecciones));
			
		}
		
		log.info("[ACCION] -- /order-proposal -- {} Ha solicitado un listado de pedidos con un permiso de {} -- {}",usrToken,rol,seguridad);
		
		return response;
	}
	
	public List<OrderDTO> getPedidosBak(Usuario usuario,String rol,String seguridad,String usrToken) throws CPException
	{
		List<PedidoBackup> pedidos = this.pedidoBakRepo.findByUsuarioPedidoBack(usuario);
		List<OrderDTO> response = new LinkedList<OrderDTO>();
		
		for(PedidoBackup item:pedidos)
		{
			String uuid = item.getUuid();
			String referencia = this.encryptor.decrypt(item.getReferencia());
			String username = this.encryptor.decrypt(usuario.getUsername());
			LocalDateTime fecha = item.getFecha();
			EstadoPedido estado = item.getEstado();
			
			List<SeleccionDTO> selecciones = new LinkedList<SeleccionDTO>();
			
			for(Map<String,Object> seleccion:item.getContent())
			{
			    String referenciaOrder = this.encryptor.decrypt((String) seleccion.get("referencia"));
			    
			    Number fondoNum = (Number) seleccion.get("fondo");
			    float fondo = fondoNum.floatValue();
			    
			    Number anchoNum = (Number) seleccion.get("ancho");
			    float ancho = anchoNum.floatValue();
			    
			    Number altoNum = (Number) seleccion.get("alto");
			    float alto = altoNum.floatValue();
			    
			    String armazon = this.encryptor.decrypt((String) seleccion.get("armazon"));
			    String colorArmazon = this.encryptor.decrypt((String) seleccion.get("colorArmazon"));
			    
			    String frente = "-";
			    String acabadoFrente = "-";
			    String colorFrente = "-";
			    
			    if( !((String)(seleccion.get("frente"))).equals("-"))
			    {
			    	 frente = this.encryptor.decrypt((String) seleccion.get("frente"));
				     acabadoFrente = this.encryptor.decrypt((String) seleccion.get("acabadoFrente"));
				     colorFrente = this.encryptor.decrypt((String) seleccion.get("colorFrente"));
			    }
			    
			   
			    String acabadoRegleta = this.encryptor.decrypt((String) seleccion.get("acabadoRegleta"));
			    String colorRegleta = this.encryptor.decrypt((String) seleccion.get("colorRegleta"));
			    String acabadoTirador = this.encryptor.decrypt((String) seleccion.get("acabadoTirador"));
			    String colorTirador = this.encryptor.decrypt((String) seleccion.get("colorTirador"));
			    			    
			    List<String> extras = new ObjectMapper().convertValue(seleccion.get("extras"), new TypeReference<List<String>>() {});
			    
			    List<String> extrasDecrypt = new LinkedList<>();
			    
			    for(String extra : extras) 
			    {
			        extrasDecrypt.add(this.encryptor.decrypt(extra));
			    }
			    
			    String serie = this.encryptor.decrypt((String) seleccion.get("serie"));
			    String observaciones = "";
			    
			    if(!((String) seleccion.get("observaciones")).isBlank())
			    {
			    	observaciones = this.encryptor.decrypt((String) seleccion.get("observaciones"));
			    }
			    			    
			    Number precioFinalNum = (Number) seleccion.get("precioFinal");
			    float precioFinal = precioFinalNum.floatValue();
			    
			    Number cantidadNum = (Number) seleccion.get("cantidad");
			    int cantidad = cantidadNum.intValue();
			    
			    boolean isEspecial = (boolean) seleccion.get("isEspecial");
			    
			    String tipo = (String) seleccion.get("tipo");
			    
			    SeleccionDTO seleccionDto = new SeleccionDTO(null, referenciaOrder, username, serie, fondo, ancho, alto, null, armazon, colorArmazon, null, frente, acabadoFrente, colorFrente, null, acabadoTirador, colorTirador, null, acabadoRegleta, colorRegleta, extrasDecrypt, precioFinal, cantidad, observaciones, isEspecial, tipo,null, null);
			    
			    selecciones.add(seleccionDto);
			}
			
			OrderDTO order = new OrderDTO(item.getUuid(), referencia, username, fecha, null, estado, selecciones);
			
			if(!this.pedidoRepo.findById(uuid).isPresent())
			{
				response.add(order);
			}
			
		}
		
		log.info("[ACCION] -- /order-proposal-bak -- {} Ha solicitado un listado de copias de pedido con permiso de {} -- {}",usrToken,rol,seguridad);
		
		return response;
		
	}
	
	public void deletePedido(String uuid,String rol,String seguridad,String usrToken) throws CPException
	{
		Optional<Pedido> pedidoOpt = this.pedidoRepo.findById(uuid);
		
		if(!pedidoOpt.isPresent())
		{
			log.warn("[AVISO] -- /orders -- {} Ha intentado borrar un pedido insxistente con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Datos inexistentes");
		}
		
		Pedido pedido = pedidoOpt.get();
		
		log.info("[ADMIN] -- /orders -- {} Ha borrado el pedido {} de la base de datos con permiso de {} -- {}",usrToken,pedido.getUuid(),rol,seguridad);
		
		this.pedidoRepo.delete(pedido);
		this.pedidoRepo.flush();
		
		Usuario userPedido = pedido.getUsuarioPedido();
		
		userPedido.removePedidos(pedido);
		
		List<ProductoConfigurado> affected = new LinkedList<ProductoConfigurado>();
		
		for(String id:pedido.getProductos())
		{
			Optional<ProductoConfigurado> itemOpt = this.seleccionRepo.findById(id);
			
			if(itemOpt.isPresent())
			{
				ProductoConfigurado item = itemOpt.get();
				affected.add(item);
				userPedido.removeProducto(item);
			}
		}
		
		List<BulkProductosUsuario> bulksAffected = new LinkedList<BulkProductosUsuario>();
		
		for(BulkProductosUsuario item:this.bulkRepo.findAll()) 
		{
			List<String> productos = item.getProductos();
		    List<String> productosPedido = pedido.getProductos();
		    
		    productos.sort((a, b) -> b.compareTo(a));
		    productosPedido.sort((a, b) -> b.compareTo(a));
		   
		    
		    if(productos.size() == productosPedido.size() && productos.equals(productosPedido)) 
		    {
			    String userUuid = userPedido.getUuid();
		    	if(userUuid.equals(item.getUsuarioUuid().getUuid()) && this.encryptor.decrypt(item.getReferencia()).equals(this.encryptor.decrypt(pedido.getReferencia())))
		    	{
			    	bulksAffected.add(item); 
			    	userPedido.removeBulk(item);
		    	}
		    }
			
			
		}
		
		log.info("[ADMIN] -- /orders -- {} Se han borrado {} selecciones asociadas al pedido {} borrado con permiso de  {} -- {}",usrToken,affected.size(),pedido.getUuid(),rol,seguridad);
		
		this.seleccionRepo.deleteAll(affected);
		this.seleccionRepo.flush();
		
		this.bulkRepo.deleteAll(bulksAffected);
		this.bulkRepo.flush();
		
		this.userRepo.save(userPedido);
		this.userRepo.flush();
		
		
	}
	
	public void deletePedidoBak(String uuid,String rol,String seguridad,String usrToken) throws CPException
	{
		Optional<PedidoBackup> pedidoOpt = this.pedidoBakRepo.findById(uuid);
		
		if(!pedidoOpt.isPresent())
		{
			log.warn("[AVISO] -- /order-proposal-bak -- {} Ha intentado borrar una copia de pedido insxistente con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Datos inexistentes");
		}
		
		PedidoBackup pedido = pedidoOpt.get();
		
		log.info("[ADMIN] -- /order-proposal-bak -- {} Ha borrado la copia del pedido {} de la base de datos con permiso de {} -- {}",usrToken,pedido.getUuid(),rol,seguridad);
		
		this.pedidoBakRepo.delete(pedido);
		this.pedidoBakRepo.flush();
		
		Usuario userPedido = pedido.getUsuarioPedidoBack();
		
		userPedido.removePedidoBak(pedido);
		
		this.userRepo.save(userPedido);
		this.userRepo.flush();
	}
	
	public void actualizarEstado(OrderDTO body,String uuid,String rol,String seguridad,String usrToken) throws CPException
	{
		if(body == null) 
		{
			throw new CPException(400, "Datos invalidos");
		}
		
		EstadoPedido estado = body.getEstado();
		String referencia = null;
		
		if(!rol.equals(CPConstants.ADMIN_ROLE) && !rol.equals(CPConstants.SUPADMIN_ROLE))
		{
			if(!estado.equals(EstadoPedido.CURSADO))
			{
				log.warn("[AVISO] -- /orders -- {} Ha intentado actualizar el estado de un pedido al valor {} el cual no está permitido con permiso de {} -- {}",usrToken,estado,rol,seguridad);
				throw new CPException(400,"Datos invalidos");
			}
		}
		else
		{
			// Array con los estados válidos para administradores
			EstadoPedido[] estados = {EstadoPedido.NO_CURSADO, EstadoPedido.CURSADO};
			Arrays.sort(estados);
			if(Arrays.binarySearch(estados, estado) == -1)
			{
				log.warn("[AVISO] -- /orders -- {} Ha intentado actualizar el estado de un pedido al valor {} el cual no es válido con permiso de {} -- {}",usrToken,estado,rol,seguridad);
				throw new CPException(400,"Datos invalidos");
			}
			
			// Los administradores puede actualizar la referencia del pedido
			referencia = body.getReferencia();
			
			if(referencia!=null)
			{
				if(!referencia.isBlank())
				{
					referencia = this.encryptor.encrypt(referencia);
				}
			}
		}
		
		Optional<Pedido> pedidoOpt = this.pedidoRepo.findById(uuid);
		
		if(!pedidoOpt.isPresent())
		{
			log.warn("[AVISO] -- /orders -- {} Ha intentado actualizar el estado de un pedido inexistente con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Datos inexistentes");
		}
		
		Pedido pedido = pedidoOpt.get();
		
		pedido.setEstado(estado);
		
		if(referencia!=null)
		{
			pedido.setReferencia(referencia);
		}
		
		log.info("[ACCION] -- /orders -- {} Ha actualizado el estado del pedido {} a {} con permiso de {} -- {}",usrToken,pedido.getUuid(),estado,rol,seguridad);
		
		this.pedidoRepo.save(pedido);
		this.pedidoRepo.flush();
		
		Usuario userPedido = pedido.getUsuarioPedido();
		userPedido.removePedidos(pedido);
		userPedido.addPedidos(pedido);
		
		this.userRepo.save(userPedido);
		this.userRepo.flush();
	}
	
	public void sendPedido(MultipartFile file,Usuario usuario,String uuid,String seguridad) throws CPException
	{
		Optional<Pedido> optPedido = this.pedidoRepo.findById(uuid);
		
		if(!optPedido.isPresent())
		{
			log.warn("[AVISO] -- /order-proposal/send -- {} Ha tratado de mandar un pedido inexistente con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		Pedido pedido = optPedido.get();
		
		String referencia = this.encryptor.decrypt(pedido.getReferencia());
		String username = this.encryptor.decrypt(usuario.getUsername());
		
		byte [] pdfBytes = null;
		
		try
		{
			pdfBytes = file.getBytes();
		}
		catch(IOException ex)
		{
			log.warn("[AVISO] -- /order-proposal/send -- {} Ha introducido un pdf corrupto debido a que no se puede transformar a byte[] con permiso de {} -- {}",usuario.getUSRToken(),usuario.getRol().getNombre(),seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		this.mailService.sendOrderMail(pdfBytes, username, referencia, pedido.getFecha(), "desarrolloit@a3com.es", usuario.getUSRToken(), seguridad);
		
		log.info("[ACCION -- /order-proposal/send -- {} Ha enviado el pedido {} correctamente a fábrica con permiso de {} -- {}",usuario.getUSRToken(),uuid,usuario.getRol().getNombre(),seguridad);
		
		pedido.setEstado(EstadoPedido.CURSADO);
		this.pedidoRepo.save(pedido);
	}
	
	private void registerPedidoBak(Usuario usuario,Pedido pedido,List<ProductoConfigurado> selecciones,String rol,String seguridad,String usrToken)
	{
		PedidoBackup pedidoBak = new PedidoBackup();
		
		pedidoBak.setUuid(pedido.getUuid());
		pedidoBak.setReferencia(pedido.getReferencia());
		pedidoBak.setEstado(pedido.getEstado());
		pedidoBak.setFecha(pedido.getFecha());
		pedidoBak.setUsuarioPedidoBack(usuario);
		
		List<Map<String,Object>> content = new LinkedList<Map<String,Object>>();
		
		for(ProductoConfigurado item:selecciones)
		{
			Map<String,Object> selContent = new HashMap<String, Object>();
			
			String referencia = item.getConfiguracion().getReferencia();
			String frente = "-";
			String acabadoFrente = "-";
			String colorFrente = "-";
			
			if(item.getFrente()!=null)
			{
				Frente frenteItem = item.getFrente();
				referencia =  this.encryptor.decrypt(item.getFrente().getReferencia()) + " " + referencia;
				frente = frenteItem.getNombre();
				acabadoFrente = item.getAcabadoFrente().getNombre();
				colorFrente = item.getColorFrente().getNombre();
			}
			
			referencia = this.encryptor.encrypt(referencia);
			
			String acabadoTirador = null;
			String acabadoRegleta = null;
			String colorTirador = null;
			String colorRegleta = null;
			
			if(item.getAcabadoTirador()!=null)
			{
				acabadoTirador = item.getAcabadoTirador().getNombre();
				if(item.getColorTirador()!=null)
				{
					colorTirador = item.getColorTirador().getNombre();
				}
				else
				{
					colorTirador = item.getCodigoColorTirador();
				}
			}
			
			if(item.getAcabadoRegleta() != null)
			{
				acabadoRegleta = item.getAcabadoRegleta().getNombre();
				if(item.getColorRegleta()!=null)
				{
					colorRegleta = item.getColorRegleta().getNombre();
				}
				else
				{
					colorRegleta = item.getCodigoColorTirador();
				}
		    }
			
			String colorArmazon = "Sin color";
			
			if(item.getCodigoColorArmazon()==null)
			{
				colorArmazon =  item.getColorArmazon().getNombre();
			}
			else
			{
				colorArmazon = item.getCodigoColorArmazon();
			}
			
			// Estos ternarios asignan el valor las medidas del producto configurado que serían las medidas especiales, en caso de ser nulas, se asignan la de la referencia escogida
			float fondo = item.getFondo() != null ? item.getFondo() : item.getConfiguracion().getFondo();
			float ancho = item.getAncho() != null ? item.getAncho() : item.getConfiguracion().getAncho();
			float alto = item.getAlto() != null ? item.getAlto() : item.getConfiguracion().getAlto();
			
			// Si existen medidas especiales se marca que la configuración presenta la etiqueta ESP
			boolean isEspecial = fondo!=item.getConfiguracion().getFondo() || ancho!=item.getConfiguracion().getAncho() || alto!=item.getConfiguracion().getAlto();
			
			List<String> extrasDecrypt = new LinkedList<String>();
			
			for(String extra:item.getExtras())
			{
				extrasDecrypt.add(extra);
			}
			
			String observaciones = "";
			
			if(item.getObservaciones()!=null)
			{
				observaciones = item.getObservaciones();
			}
			
			String serie = this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getNombre());
			serie += " "+this.encryptor.decrypt(item.getConfiguracion().getSerie().getVariante());
			serie = this.encryptor.encrypt(serie);
			
			String tipo = this.encryptor.decrypt(item.getConfiguracion().getSerie().getProducto().getTipo());
			tipo = this.encryptor.encrypt(tipo);
			
			selContent.put("referencia",referencia);
			selContent.put("fondo", fondo);
			selContent.put("ancho", ancho);
			selContent.put("alto", alto);
			selContent.put("armazon", item.getAcabado().getNombre());
			selContent.put("colorArmazon", colorArmazon);
			selContent.put("frente",frente);
			selContent.put("acabadoFrente",acabadoFrente);
			selContent.put("colorFrente",colorFrente);
			selContent.put("acabadoRegleta", acabadoRegleta);
			selContent.put("colorRegleta", colorRegleta);
			selContent.put("acabadoTirador", acabadoTirador);
			selContent.put("colorTirador", colorTirador);
			selContent.put("extras", extrasDecrypt);
			selContent.put("serie", serie);
			selContent.put("observaciones", observaciones);
			selContent.put("precioFinal", item.getPrecioFinal());
			selContent.put("cantidad", item.getCantidad());
			selContent.put("isEspecial", isEspecial);
			selContent.put("tipo", tipo);
			
			content.add(selContent);
		}
		
		pedidoBak.setContent(content);
		
		usuario.addPedidoBak(pedidoBak);
		
		log.info("[ACCION] -- /order-proposal -- {} Ha creado una copia del pedido {} con permiso de {} -- {}",usrToken,pedido.getUuid(),rol,seguridad);
		
		this.pedidoBakRepo.save(pedidoBak);
		this.pedidoBakRepo.flush();
		this.userRepo.save(usuario);
		this.userRepo.flush();		
	}
	
}