package es.aag.configurador.campoaras.services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.aag.configurador.campoaras.dto.ResponseConfiguracion;
import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.entities.Acabado;
import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.Color;
import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.Frente;
import es.aag.configurador.campoaras.entities.Pedido;
import es.aag.configurador.campoaras.entities.Producto;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;
import es.aag.configurador.campoaras.entities.Serie;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IAcabadoRepository;
import es.aag.configurador.campoaras.repositories.IBulkProductosUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IColorRepository;
import es.aag.configurador.campoaras.repositories.IConfiguracionRepository;
import es.aag.configurador.campoaras.repositories.IFrenteRepository;
import es.aag.configurador.campoaras.repositories.IPedidoRepository;
import es.aag.configurador.campoaras.repositories.IProductoConfiguradoRepository;
import es.aag.configurador.campoaras.repositories.ISerieRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import es.aag.configurador.campoaras.utils.Validations;

/**
 * Servicio encargado de la gestión de configuraciones de la app
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 */
@Service
public class ConfigurationService 
{
private Logger log = LogManager.getLogger();
	
	@Autowired
	private EncryptorService encryptor;
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private ISerieRepository seriesRepo;
	
	@Autowired
	private IAcabadoRepository acabadoRepo;
	
	@Autowired
	private IColorRepository colorRepo;
	
	@Autowired
	private IFrenteRepository frenteRepo;
	
	@Autowired
	private IConfiguracionRepository configuracionRepo;
	
	@Autowired
	private IProductoConfiguradoRepository seleccionRepo;
	
	@Autowired
	private IBulkProductosUsuarioRepository bulkRepo;
	
	@Autowired
	private IPedidoRepository orderRepo;
	
	private final Validations validation;
	
	public ConfigurationService()
	{
		this.validation = new Validations();
	}
	
	/**
	 * Metodo que devuelve, registra, actualiza y borra una configuración dependiendo del endpoint a consultar, en caso de ser GET devuelve una lista de producto , si es otro método devuelve null
	 * @param body
	 * @param referencia
	 * @param method
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 * @throws CPException
	 */
	public List<ResponseConfiguracion> manageConfiguracion (ResponseConfiguracion body,String referencia, String uuid,String method,String rol,String seguridad,String usrToken) throws CPException
	{
		this.validation.initialize(null, null, acabadoRepo, null, seriesRepo, configuracionRepo, encryptor);
		
		List<ResponseConfiguracion> response = null;
		
		switch(method)
		{
			case CPConstants.POST:
			{
				if(!this.validation.validateConfiguration(body))
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha introducido datos erroneos para subir una configuracion con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Optional<Configuracion> configOpt = this.configuracionRepo.findById(body.getReferencia());
				
				if(configOpt.isPresent())
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha intentado añadir una configuracion {} que ya existe con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
					throw new CPException(409,"No tienes permiso");
				}
				
				List<Map<String,Object>> armazonSaneado = this.validation.sanearArmazon(body.getArmazon());
				List<Map<String,Object>> extrasSaneado  = this.validation.sanearExtras(body.getExtras());
				
				if(armazonSaneado.size()==0)
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha introducido un armazon erroneo en la configuracion con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				// Ternario para mantener el valor de extrasSaneado en caso de que su tamaño sea > 0
				extrasSaneado = extrasSaneado.size()>0 ? extrasSaneado : null; 
				
				Configuracion config = new Configuracion();
				
				config.setReferencia(body.getReferencia());
				config.setFondo(body.getFondo());
				config.setAncho(body.getAncho());
				config.setAlto(body.getAlto());
				config.setAltoMax(body.getAltoMax());
				config.setFondoMin(body.getFondoMin());
				config.setFondoMax(body.getFondoMax());
				config.setPrecioMedidaFondoEsp(body.getPrecioMedidaFondoEsp());
				config.setPrecioMedidaAnchoEsp(body.getPrecioMedidaAnchoEsp());
				config.setPrecioMedidaAltoEsp(body.getPrecioMedidaAltoEsp());
				config.setArmazon(armazonSaneado);
				config.setExtras(extrasSaneado);
				
				Serie serie = this.validation.findSerie(body.getSerie());
				
				if(serie==null)
				{
					Optional<Serie> serieOpt = this.seriesRepo.findById(body.getSerie());
					
					if(!serieOpt.isPresent())
					{
						log.warn("[AVISO] -- /configuracion -- {} Ha introducido una serie erronea en una configuración con permiso de {} -- {}",usrToken,rol,seguridad);
						throw new CPException(400,"Datos invalidos");
					}
					
					serie = serieOpt.get();
				}
				
				config.setSerie(serie);

				log.info("[ADMIN] -- /configuracion -- {} Ha añadido la configuracion {} a la base de datos con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
				
				this.configuracionRepo.save(config);
				
				serie.addConfiguracion(config);
				
				this.seriesRepo.save(serie);				
				break;
			}
			case CPConstants.GET:
			{
				response = new LinkedList<ResponseConfiguracion>();
				List<Configuracion> configuraciones = this.configuracionRepo.findAll();
				
				for(Configuracion config:configuraciones)
				{
					if(uuid!=null && !config.getSerie().getUuid().equals(uuid))
					{
						continue;
					}
					
					String serie = this.encryptor.decrypt(config.getSerie().getProducto().getNombre());
					serie+= " "+this.encryptor.decrypt(config.getSerie().getVariante());
					List<Map<String,Object>> armazon = new LinkedList<Map<String,Object>>();
					
					for(Map<String,Object> item:config.getArmazon())
					{
						String nombre = (String) item.get("nombre");
						item.put("nombre", this.encryptor.decrypt(nombre));
						armazon.add(item);
					}
					
					List<Map<String,Object>> extras = new LinkedList<Map<String,Object>>();
					
					if(config.getExtras()!=null)
					{
						
						for(Map<String,Object> item:config.getExtras())
						{
							String nombre = (String) item.get("nombre");
							item.put("nombre", this.encryptor.decrypt(nombre));
							extras.add(item);
						}
					}
					
					ResponseConfiguracion configDTO = new ResponseConfiguracion(config.getReferencia(), config.getFondo(),
							config.getAncho(), config.getAlto(),config.getAltoMax(),config.getFondoMin(),config.getFondoMax(),config.getPrecioMedidaFondoEsp(),config.getPrecioMedidaAnchoEsp(),config.getPrecioMedidaAltoEsp() , armazon, extras, serie);
					response.add(configDTO);
				}
				
				log.info("[ACCION] -- /configuracion -- {} Ha solicitado un listado de configuraciones con permiso de {} -- {}",usrToken,rol,seguridad);
				break;
			}
			case CPConstants.PATCH:
			{
				Optional<Configuracion> configOpt = this.configuracionRepo.findById(referencia);
				
				if(!configOpt.isPresent())
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha solicitado una configuracion que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
								
				if(!this.validation.validateConfiguration(body))
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha introducido datos erroneos para subir una configuracion con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Configuracion config = configOpt.get();
				
				List<Map<String,Object>> armazonSaneado = this.validation.sanearArmazon(body.getArmazon());
				List<Map<String,Object>> extrasSaneado = this.validation.sanearExtras(body.getExtras());

				if(armazonSaneado.size()==0)
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha introducido un armazon erroneo en la configuracion con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				// Ternario para mantener el valor de extrasSaneado en caso de que su tamaño sea > 0
				extrasSaneado = extrasSaneado.size()>0 ? extrasSaneado : null; 
				
				config.setFondo(body.getFondo());
				config.setAncho(body.getAncho());
				config.setAlto(body.getAlto());
				config.setAltoMax(body.getAltoMax());
				config.setFondoMin(body.getFondoMin());
				config.setFondoMax(body.getFondoMax());
				config.setPrecioMedidaFondoEsp(body.getPrecioMedidaFondoEsp());
				config.setPrecioMedidaAnchoEsp(body.getPrecioMedidaAnchoEsp());
				config.setPrecioMedidaAltoEsp(body.getPrecioMedidaAltoEsp());
				config.setArmazon(armazonSaneado);
				config.setExtras(extrasSaneado);
				
				Serie serie = this.validation.findSerie(body.getSerie());
				
				if(serie==null)
				{
					Optional<Serie> serieOpt = this.seriesRepo.findById(body.getSerie());
					
					if(!serieOpt.isPresent())
					{
						log.warn("[AVISO] -- /configuracion -- {} Ha introducido una serie erronea en una configuración con permiso de {} -- {}",usrToken,rol,seguridad);
						throw new CPException(400,"Datos invalidos");
					}
					
					serie = serieOpt.get();
				}
				
				config.setSerie(serie);
				
				log.info("[ADMIN] -- /configuracion -- {} Ha actualizado la configuracion {} a la base de datos con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
				
				this.configuracionRepo.save(config);
				
				serie.addConfiguracion(config);
				
				this.seriesRepo.save(serie);	
				
				break;
			}
			case CPConstants.DELETE:
			{
				Optional<Configuracion> configOpt = this.configuracionRepo.findById(referencia);
				
				if(!configOpt.isPresent())
				{
					log.warn("[AVISO] -- /configuracion -- {} Ha solicitado una configuracion que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Configuracion toDelete = configOpt.get();
				
				List<ProductoConfigurado> selecciones = new LinkedList<ProductoConfigurado>(); 
				List<BulkProductosUsuario> allBulks = this.bulkRepo.findAll();
				Set<BulkProductosUsuario> bulks = new HashSet<BulkProductosUsuario>(); 
				List<Pedido> allPedidos = this.orderRepo.findAll();
				Set<Pedido> pedidos = new HashSet<Pedido>(); 
				
				List<ProductoConfigurado> selHeredado = this.seleccionRepo.findByConfiguracion(toDelete);
				selecciones.addAll(selHeredado);
				
				for(ProductoConfigurado seleccion:selHeredado)
				{
					for(BulkProductosUsuario bulk:allBulks)
					{
						if(bulk.getProductos().contains(seleccion.getUuid()))
						{
							bulks.add(bulk);
							break;
						}
					}
					
					for(Pedido pedido:allPedidos) 
					{
						if(pedido.getProductos().contains(seleccion.getUuid()))
						{
							pedidos.add(pedido);
							break;
						}
					}

				}
				
				
				
				
				this.orderRepo.deleteAll(pedidos);
				this.orderRepo.flush();
				
				this.bulkRepo.deleteAll(bulks);
				this.bulkRepo.flush();
				
				this.seleccionRepo.deleteAll(selecciones);
				this.seleccionRepo.flush();
				
				log.info("[ADMIN] -- /configuracion -- {} Ha eliminado la configuracion {} de la base de datos con permiso de {} -- {}",usrToken,toDelete.getReferencia(),rol,seguridad);
				this.configuracionRepo.delete(toDelete);
				
				break;
			}
			default:
			{
				log.warn("[AVISO] -- /configuracion -- {} Ha intentado acceder a la gestion de configuraciones sin un metodo valido con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
		}
		
		this.validation.destroy();
		
		return response;
			
	}
	
	/**
	 * Metodo que permite que un usuario configure un producto en base a la referencia escogida
	 * @param body
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @param usuario
	 * @throws CPException
	 */
	@Transactional
	public String configureProduct(SeleccionDTO body,String rol,String seguridad,String usrToken,Usuario usuario) throws CPException
	{
		this.validation.initialize(null, frenteRepo, acabadoRepo, colorRepo, seriesRepo, configuracionRepo, encryptor);

		if(!rol.equals(CPConstants.SUPADMIN_ROLE) &&  !rol.equals(CPConstants.ADMIN_ROLE) && !rol.equals(CPConstants.CLIENTE_ROLE))
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado acceder a la gestión de configuraciones con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		String referencia = "";
		
		if(body.getReferencia()==null && (body.getAncho()!=null || body.getFondo()!=null))
		{
			referencia = this.chooseReferencia(body.getSerie(), body.getAncho(),body.getFondo());
		}
		else
		{
			referencia = body.getReferencia();
		}
		
		Optional<Serie> serieOpt = this.seriesRepo.findById(body.getSerie());
		
		if(!serieOpt.isPresent())
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto introduciendo una variante erronea con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Se debe de introducir una variante para continuar");
		}
		
		Serie serie = serieOpt.get();
		List<Configuracion> configuraciones = this.configuracionRepo.findAll();
		List<Map<String,Object>> extras = new LinkedList<Map<String,Object>>();
		
		
		for(Configuracion config:configuraciones)
		{			
			if(!config.getSerie().getUuid().equals(serie.getUuid()) || config.getExtras()==null)
			{
				continue;
			}	
			if(!config.getExtras().isEmpty())
			{
				extras.addAll(config.getExtras());
			}
		}
		
		
		if(referencia.equals("error"))
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con una medida de ancho erronea con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"El ancho seleccionado es erroneo");
		}
		
		// Apartado de cesta o bulk para validar que la referencia no venga vacía a la hora de configurar una cesta
		if(body.getBulk()==null)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto sin especificar añadir/finalizar un bulk con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Se debe indicar continuar o finalizar compra");
		}
		
		String referenciaBulk = "";
		
		if(!body.getBulk() && body.getReferenciaBulk()==null)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto sin especificar la referencia de un bulk con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Se debe indicar la referencia de la cesta");
		}
		
		if(!body.getBulk() && body.getReferenciaBulk().isBlank())
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto sin especificar la referencia de un bulk con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Se debe indicar la referencia de la cesta");
		}
		
		referenciaBulk = body.getReferenciaBulk();
		
		Optional<Configuracion> configuracionOpt = this.configuracionRepo.findById(referencia);
		
		if(!configuracionOpt.isPresent())
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con una referencia de configuración erronea con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Las medidas seleccionadas no presentan ninguna configuracion");
		}
		
		Configuracion config = configuracionOpt.get();
		
		// Fase de validacion de medidas
		
		if(body.getAlto()!=null)
		{
			if(body.getAlto()>config.getAltoMax() && body.getAlto()<config.getAlto())
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con una medida de alto erronea con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(400,"El alto seleccionado es erroneo");
			}
		}
		
		ProductoConfigurado seleccion = new ProductoConfigurado();
		
		String uuid = UUID.randomUUID().toString();		
		
		// Fase de recuperación de entidades
		
		Acabado armazon = this.validation.findAcabado(body.getArmazon());
		
		if(armazon==null)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un armazón erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Se ha seleccionado un acabado erroneo para el armazon dado");
		}
		
		Color colorArmazon = this.validation.findColor(body.getColorArmazon());
		
		if(colorArmazon==null)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un color de armazon erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Se ha seleccionado un color de armazon erroneo");
		}
		
		Producto producto = config.getSerie().getProducto();
		Frente frente = this.validation.findFrentes(body.getFrente());
		
		if(frente == null && producto.getFrentesProductos().size()>0)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un frente erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Se ha seleccionado un frente erroneo para el armazon dado");
		}
			
		Acabado acabadoFrente = this.validation.findAcabado(body.getAcabadoFrente());
		
		if(acabadoFrente == null && producto.getFrentesProductos().size()>0)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un armazon de frente erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Se ha seleccionado un acabado erroneo para el frente dado");
		}
		
		Color colorFrente = this.validation.findColor(body.getColorFrente());
		
		if(colorFrente==null && producto.getFrentesProductos().size()>0)
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un color de frente erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Se ha seleccionado un color de frente erroneo");
		}
		
		Acabado acabadoTirador = this.validation.findAcabado(body.getAcabadoTirador());
		
		Acabado acabadoRegleta = this.validation.findAcabado(body.getAcabadoRegleta());
		
		Color colorTirador = this.validation.findColor(body.getColorTirador());
		
		Color colorRegleta = this.validation.findColor(body.getColorRegleta());
		
		if(body.getAcabadoTirador() != null) 
		{
			if(acabadoTirador==null)
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un acabado de tirador erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(404,"Se ha seleccionado un acabado erroneo para el tirador dado");
			}
			
			if(colorTirador==null)
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un color de tirador erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(404,"Se ha seleccionado un color de tirador erroneo");
			}
				
		}
		
		if(body.getAcabadoRegleta() != null) 
		{
			if(acabadoRegleta==null)
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un acabado de regleta erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(404,"Se ha seleccionado un acabado erroneo para la regleta dada");
			}
			
			if(colorRegleta==null)
			{
				log.warn("[AVISO] -- /configure -- {} Ha intentado configurar un producto con un color de regleta erroneo con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(404,"Se ha seleccionado un color de regleta erroneo");
			}
		}
		
		// Fase de validacion de entidades
				
		List<Map<String,Object>> acabadoConfig = config.getArmazon();
		float precioArmazon = this.validateAcabado(acabadoConfig, body.getArmazon());
		
		if(precioArmazon==-1)
		{
			log.warn("[AVISO] -- /configure -- {} Ha configurado un producto con un armazón erroneo en la referencia {} con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
			throw new CPException(400,"El armazon seleccionado no existe dentro de la configuracion dada");
		}
		
		if(!armazon.getColores().contains(colorArmazon))
		{
			log.warn("[AVISO] -- /configure -- {} Ha configurado un producto con un color que no existe en el armazón dado en la referencia {} con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
			throw new CPException(400,"El color del armazon dado no se encuentra en la configuración dada");
		}
		
		if(!config.getSerie().getProducto().getFrentesProductos().contains(frente) && producto.getFrentesProductos().size()>0)
		{
			log.warn("[AVISO] -- /configure -- {} Ha configurado un producto con un frente erroneo en la referencia {} con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
			throw new CPException(400,"El frente seleccionado no se encuentra en el producto configurado");
		}
				
		float precioFrente = 0;
		float precioTirador = 0;
		float precioRegleta = 0;
				
		if(producto.getFrentesProductos().size()>0)
		{
			precioFrente = this.validateAcabado(acabadoConfig, body.getAcabadoFrente());
			
			if(precioFrente==-1)
			{
				log.warn("[AVISO -- /configure -- {} Ha configurado un producto con un armazón de frente inexistente en la referencia {} con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
				throw new CPException(400,"El acabado del frente ni existe dentro de la configuracion dada");
			}
			
			if(!acabadoFrente.getColores().contains(colorFrente))
			{
				log.warn("[AVISO -- /configure -- {} Ha configurado un producto con un color que no existe en el frente dado en la referencia {} con permiso de {} -- {}",usrToken,body.getReferencia(),rol,seguridad);
				throw new CPException(400,"El color del frente dado no se encuentra en la configuración dada");
			}
			// Se setean los precios de regleta y tirador a 0 para evitar sumas incorrectas en el precio final
			precioRegleta = 0;
			precioTirador = 0;
		}
		
		float precioFinal = 0;
		
		// Si hay acabados especiales, en vez de elegir el precio más alto se suman
		try
		{
			if(this.isEspecial(armazon.getTipos()) || this.isEspecial(acabadoFrente.getTipos()))
			{
				precioFinal += precioArmazon + precioFrente;
			}
			else
			{
				// Ternario para definir el precio más alto del armazon o el acabado del frente
				precioFinal = precioArmazon > precioFrente ? precioArmazon : precioFrente;
			}
		}
		catch(NullPointerException ex)
		{
			// En caso de que no haya frente acabadoFrente.getTipos() es nulo, por lo que se coge el precio más alto
			precioFinal = precioArmazon > precioFrente ? precioArmazon : precioFrente;
		}
		
		float fondo = config.getFondo();
		float ancho = config.getAncho();
		float alto = config.getAlto();
		
		if(body.getFondo()!=null)
		{
			if(body.getFondo().floatValue() != fondo)
			{
				precioFinal+=config.getPrecioMedidaFondoEsp();
				fondo = body.getFondo();
				seleccion.setFondo(fondo);
			}
		}
		
		if(body.getAncho()!=null)
		{
			if(body.getAncho().floatValue() != ancho)
			{
				precioFinal+=config.getPrecioMedidaAnchoEsp();
				ancho = body.getAncho();
				seleccion.setAncho(ancho);
			}
		}
		
		if(body.getAlto()!=null)
		{
			if(body.getAlto().floatValue() != alto)
			{
				precioFinal+=config.getPrecioMedidaAltoEsp();
				alto = body.getAlto();
				seleccion.setAlto(alto);
			}
		}
		
		List<String> extrasSeleccion = new LinkedList<String>();
		
		if(!extras.isEmpty() && !body.getExtras().isEmpty())
		{
			for(Map<String,Object> extra:extras)
			{
				String nombre = this.encryptor.decrypt((String) extra.get("nombre"));
				
				if(body.getExtras().contains(nombre))
				{
					Number rawPrecio = (Number) extra.get("precio");
					// Si en el extra se indica porcentaje se realiza el porcentaje sobre el precio final marcado
					if(nombre.contains("(%)"))
					{
						precioFinal = precioFinal + ((precioFinal * rawPrecio.floatValue()) / 100);
					}
					else
					{
						precioFinal+=rawPrecio.floatValue();
					}
					
					extrasSeleccion.add(this.encryptor.encrypt(nombre));
				}
			}
		}
		
		precioFinal = (precioFinal * body.getCantidad());
		// Fase de inserción de datos
		
		seleccion.setUuid(uuid);
		seleccion.setConfiguracion(config);
		seleccion.setUsuario(usuario);
		seleccion.setFrente(frente);
		seleccion.setAcabado(armazon);
		seleccion.setAcabadoFrente(acabadoFrente);
		seleccion.setAcabadoTirador(acabadoTirador);
		seleccion.setAcabadoRegleta(acabadoRegleta);
		seleccion.setColorArmazon(colorArmazon);
		seleccion.setColorFrente(colorFrente);
		seleccion.setColorRegleta(colorRegleta);
		seleccion.setColorTirador(colorTirador);
		seleccion.setPrecioArmazon(precioArmazon);
		seleccion.setPrecioFrente(precioFrente);
		seleccion.setPrecioTirador(precioTirador);
		seleccion.setPrecioRegleta(precioRegleta);
		seleccion.setExtras(extrasSeleccion);
		seleccion.setPrecioFinal(precioFinal);
		seleccion.setCantidad(body.getCantidad());
		seleccion.setFecha(LocalDateTime.now());
		
		// Antes de salvar la configuracion se comprueba que el uuid del body en caso de que no venga vacío sea correcto
		BulkProductosUsuario bulk = null;
		
		if(body.getUuid()!=null)
		{
			Optional<BulkProductosUsuario> optBulk = this.bulkRepo.findById(body.getUuid());
			
			if(!optBulk.isPresent())
			{
				log.warn("[AVISO] -- /configure -- {} Ha introducido un uuid erroneo para obtener un conjunto de productos (bulk) con permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
			
			bulk = optBulk.get();
		}
		
		
		log.info("[ACCION] -- /configure -- {} Ha configurado un producto con referencia {} con permiso de {} -- {}",usrToken,config.getReferencia(),rol,seguridad);
		this.seleccionRepo.save(seleccion);
		this.seleccionRepo.flush();
		usuario.addProducto(seleccion);
		
		// Se continua el flujo anterior, si el uuid viene vacío se crea un bulk nuevo
		
		
		if(bulk == null)
		{
			boolean end = body.getBulk() != null ? !body.getBulk() : true;
			bulk = new BulkProductosUsuario();
			List<String> selecciones = List.of(seleccion.getUuid());
			
			if(!end && referenciaBulk.isBlank())
			{
				referenciaBulk = this.encryptor.encrypt(this.setDefaultReferencia(usuario));
			}
			else if(!end && !referenciaBulk.isBlank())
			{
				referenciaBulk = this.encryptor.encrypt(body.getReferenciaBulk());
			}
			else if(end && !referenciaBulk.isBlank())
			{
				referenciaBulk = this.encryptor.encrypt(body.getReferenciaBulk());
			}
			
			uuid = UUID.randomUUID().toString();
			
			bulk.setUuid(uuid);
			bulk.setUsuarioUuid(usuario);
			bulk.setReferencia(referenciaBulk);
			bulk.setProductos(selecciones);
			bulk.setEnd(end);
			bulk.setFecha(LocalDateTime.now());
			this.bulkRepo.save(bulk);
			this.bulkRepo.flush();
			usuario.addBulk(bulk);
		}
		else if(body.getBulk())
		{
			if(referenciaBulk.isBlank())
			{
				referenciaBulk = this.encryptor.encrypt(this.setDefaultReferencia(usuario));
			}
			else
			{
				referenciaBulk = this.encryptor.encrypt(body.getReferenciaBulk());
			}
			
			List<String> selecciones = bulk.getProductos();
			selecciones.add(seleccion.getUuid());
			bulk.setReferencia(referenciaBulk);
			bulk.setEnd(false);
			bulk.setFecha(LocalDateTime.now());
			this.bulkRepo.save(bulk);
			this.bulkRepo.flush();
			usuario.addBulk(bulk);
		}
		else
		{
			referenciaBulk = this.encryptor.encrypt(body.getReferenciaBulk());
			List<String> selecciones = bulk.getProductos();
			selecciones.add(seleccion.getUuid());
			bulk.setReferencia(referenciaBulk);
			bulk.setEnd(true);
			bulk.setFecha(LocalDateTime.now());
			this.bulkRepo.save(bulk);
			this.bulkRepo.flush();
			usuario.addBulk(bulk);
		}
		
		this.userRepo.save(usuario);
		this.userRepo.flush();
		
		String valueReturn = body.getUuid()!=null ? "" : bulk.getUuid();
		
		return valueReturn;
	
	}
	
	public List<ResponseSeleccion> getSelecciones(Boolean isEnd,String uuidBulk,Usuario usuario,String rol, String seguridad,String usrToken)
	{
		List<ResponseSeleccion> response = new LinkedList<ResponseSeleccion>();
		
		LocalDateTime fecha = null;		
		
		for(BulkProductosUsuario bulk:usuario.getSelecciones())
		{
			// Si hay filtro para selecciones finalizadas se aplica un continue en caso de que no se cumpla
			if(isEnd!=null)
			{
				if(bulk.isEnd()!=isEnd)
				{
					continue;
				}
			}
			
			if(uuidBulk!=null)
			{
				if(!bulk.getUuid().equals(uuidBulk))
				{
					continue;
				}
			}
			String referenciaBulk = this.encryptor.decrypt(bulk.getReferencia());
			SeleccionDTO [] selecciones = new SeleccionDTO[bulk.getProductos().size()];
			int index = 0;
			
			for(String producto:bulk.getProductos())
			{
				Optional<ProductoConfigurado> seleccionOpt = this.seleccionRepo.findById(producto);
				if(seleccionOpt.isPresent())
				{
					ProductoConfigurado item = seleccionOpt.get();
					String uuid = item.getUuid();
					String referencia = item.getConfiguracion().getReferencia();
					String armazon = this.encryptor.decrypt(item.getAcabado().getNombre());
					String colorArmazon = this.encryptor.decrypt(item.getColorArmazon().getNombre());
					String frente = "Sin frente";
					String acabadoFrente = "Sin frente";
					String colorFrente = "Sin frente";
					if(item.getFrente()!=null)
					{
						acabadoFrente = this.encryptor.decrypt(item.getAcabadoFrente().getNombre());
						colorFrente = this.encryptor.decrypt(item.getColorFrente().getNombre());
						frente = this.encryptor.decrypt(item.getFrente().getNombre());
					}
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
					
					List<String> extrasDecrypt = new LinkedList<String>();
					
					for(String extra:item.getExtras())
					{
						extrasDecrypt.add(this.encryptor.decrypt(extra));
					}
					
					SeleccionDTO seleccion = new SeleccionDTO(uuid, referencia, null,serie,fondo,ancho,alto, precioArmazon, armazon, colorArmazon,precioFrente, frente, acabadoFrente, colorFrente,precioTirador, acabadoTirador, colorTirador,precioRegleta, acabadoRegleta, colorRegleta,extrasDecrypt,precioFinal, cantidad,null,null);
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
				fecha = LocalDateTime.now();
			}
			else
			{
				fecha = bulk.getFecha();
			}
			
			ResponseSeleccion seleccion = new ResponseSeleccion(bulk.getUuid(),usuario.getUuid(),referenciaBulk,selecciones,fecha,bulk.isEnd());
			
			response.add(seleccion);
			
		}
		
		log.info("[ACCION] -- /configure -- {} Ha solicitado una lista de sus productos configurados con permiso de {} -- {}",usrToken,rol,seguridad);
		
		return response;
	}
	
	public void delSeleccion(String uuid,String rol, String seguridad,String usrToken) throws CPException
	{
		Optional<BulkProductosUsuario> optBulk = this.bulkRepo.findById(uuid);
		
		if(!optBulk.isPresent())
		{
			log.warn("[AVISO] -- /configure -- {} Ha intentado eliminar un producto configurado inexistente con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(404,"Datos no encontrados");
		}
		
		BulkProductosUsuario bulk = optBulk.get();
		
		log.info("[ACCION] -- /configure -- {} Ha eliminado el producto configurado {} con permiso de {} -- {}",usrToken,bulk.getUuid(),rol,seguridad);
		this.bulkRepo.delete(bulk);
		this.bulkRepo.flush();		
	}
	

	public String export(String uuid, String rol, String seguridad, String usrToken) throws CPException
	{
		
		List<Configuracion> configuraciones = new LinkedList<Configuracion>();
		
		if(uuid != null && !uuid.isBlank())
		{
			Optional<Serie> serieOpt = this.seriesRepo.findById(uuid);
			
			if(!serieOpt.isPresent())
			{
				log.warn("[AVISO] -- /export -- {} Ha intentado exportar configuraciones con una serie no válida con permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(400,"Datos inválidos");
			}
			
			Serie serie = serieOpt.get();
			
			for(Configuracion item:this.configuracionRepo.findAll())
			{
				if(item.getSerie().equals(serie))
				{
					configuraciones.add(item);
				}
			} 
		}
		else
		{
			log.warn("[AVISO] -- /export -- {} Ha intentado exportar configuraciones sin identificar la serie afectada con permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(400,"Datos inválidos");
		}
		
		Configuracion config = configuraciones.getFirst();
		
		List<String> cabeceras = config.getArmazon().stream()
		        .map(acabado -> (String) acabado.get("nombre"))
		        .collect(Collectors.toList());
		
		StringBuilder content = new StringBuilder();
		content.append("referencia, fondo, ancho, alto, alto max, fondo min, fondo max, fondo especial, ancho especial, alto especial");
		cabeceras.forEach(nombre -> content.append(",").append(this.encryptor.decrypt(nombre)));
		content.append("\n");
		
		
		for (Configuracion c : configuraciones) 
		{
			content.append(c.getReferencia()+",");
			content.append(c.getFondo()+",");
			content.append(c.getAncho()+",");
			content.append(c.getAlto()+",");
			content.append(c.getAltoMax()+",");
			content.append(c.getFondoMin()+",");
			content.append(c.getFondoMax()+",");
			content.append(c.getPrecioMedidaFondoEsp()+",");
			content.append(c.getPrecioMedidaAnchoEsp()+",");
			content.append(c.getPrecioMedidaAltoEsp()+",");
			
			for(String acabado:cabeceras)
			{
				for(Map<String,Object> item:c.getArmazon())
				{
					String condicion = (String) item.get("nombre");
					condicion = this.encryptor.decrypt(condicion).toLowerCase();
					
					if(condicion.equals(this.encryptor.decrypt(acabado).toLowerCase()))
					{
						Number rawValue = (Number) item.get("precio");
						float precioValue = rawValue.floatValue();
						content.append(precioValue+",");
					}
				
				}
			}
			content.append("\n");
			
		}
		return content.toString();
	}

	private float validateAcabado(List<Map<String,Object>> acabadoConfig,String acabado)
	{
		int index = 0;
		boolean found = false;
		float precio = 0;
		Number precioRaw = null;
		
		while(index<acabadoConfig.size() && !found)
		{
			Map<String,Object> item = acabadoConfig.get(index); 
			
			if(this.encryptor.decrypt( (String) item.get("nombre")).equals(acabado))
			{
				found = true;
				precioRaw = (Number) item.get("precio");
				precio = precioRaw.floatValue();
			}
			
			index++;
		}
		
		if(!found)
		{
			precio = -1;
		}
		
		return precio;
	}
	
	private boolean isEspecial(String [] tipos)
	{
		boolean especial = false;
		for(String tipo:tipos)
		{
			tipo = this.encryptor.decrypt(tipo);
			especial = tipo.equalsIgnoreCase("especial");
			if(especial)
			{
				break;
			}
		}
		
		return especial;
	}
	
	/**
	 * Metodo que escoge la referencia de un producto en base a medidas especiales
	 * @param variante
	 * @param ancho
	 * @param fondo
	 * @return
	 */
	private String chooseReferencia(String uuid,Float ancho,Float fondo)
	{
		String referencia = "";
		boolean activarFondo = false;
		
		
		Optional<Serie> serieOpt = this.seriesRepo.findById(uuid);
		
		Serie serie = serieOpt.isPresent() ? serieOpt.get() : null;
		
		List<Configuracion> referencias = List.copyOf(serie.getConfiguracion());
		
		if(referencias==null)
		{
			referencia = "error";
		}
		else if(referencias.size()==0)
		{
			referencia = "error";
		}
		
		if(fondo == null)
		{
			referencia = "error";
		}
		
		
		if(ancho!=null && !referencia.equals("error"))
		{
			float [] medidas = new float[referencias.size()];
			
			for(int i = 0;i<medidas.length;i++)
			{
				medidas[i] = referencias.get(i).getAncho();
			}
			
			Arrays.sort(medidas);
			
			if(ancho.floatValue()<medidas[0] || ancho.floatValue()>medidas[medidas.length-1])
			{
				referencia = "error";
			}
			else
			{
				int index = 0;
				float modulo = 0;
				
				// Se evita que el primer item sea 0 por división infinita
				modulo = medidas[0] != 0 ? ancho % medidas[0] : ancho;
				
				for(int i = 1;i<medidas.length;i++)
				{
					float item = medidas[i];
					
					if(item==0)
					{
						modulo = ancho;
					}
					else
					{
						if(modulo>(ancho%item))
						{
							modulo = ancho%item;
							index = i;
						}
					}	
				}
				
				Configuracion configuracion = null;
				
				for(Configuracion item:referencias)
				{
					if(item.getAncho()==medidas[index] && configuracion == null)
					{
						configuracion = item;
					}
				}
				
				// Si configuracion es nula ponemos referencia = error y nos saltamos el código
				if(configuracion == null)
				{
					referencia = "error";
				}
				
				
				if(!referencia.equals("error"))
				{
					// Si hay fondo especial aun no se asigna referencia si no se asigna
					if(fondo>configuracion.getFondoMin() && fondo<configuracion.getFondoMax())
					{
						//Se asigna el ancho colocado al ancho encontrado en la configuracion por si el usuario introduce ancho especial y fondo especial
						ancho = configuracion.getAncho();
						
						activarFondo = true;
					}
					else
					{
						//Se escoge la referencia exacta en fondo y ancho
						for(Configuracion item:referencias)
						{
							if(item.getAncho()==medidas[index] && item.getFondo()==fondo.floatValue())
							{
								referencia = item.getReferencia();
							}
						}
						
						// En caso de que sea nulo se devuelve error
						if(referencia.isBlank())
						{
							referencia = "error";
						}
						
					}
				}
				
			}
		}
		
		if(activarFondo && !referencia.equals("error"))
		{
			float fondoMax = 0;
			
			for(Configuracion item:referencias)
			{
				if(fondoMax<item.getFondo())
				{
					fondoMax = item.getFondo();
				}
			}
						
			for(Configuracion item:referencias)
			{
				if(item.getAncho()==ancho.floatValue() &&  item.getFondo()==fondoMax && referencia.isBlank())
				{
					referencia = item.getReferencia();
				}
			}
			
		}
		
		return referencia;
	}
	
	private String setDefaultReferencia(Usuario usuario)
	{
		String referenciaBulk = "";
		Set<BulkProductosUsuario> bulks = usuario.getSelecciones() ;
		int [] referencias = new int[0];
		for(BulkProductosUsuario item:bulks)
		{
			if(item.getReferencia()!=null)
			{
				if(!item.getReferencia().isBlank())
				{
					String refTemp = this.encryptor.decrypt(item.getReferencia());
					if(refTemp.startsWith(CPConstants.REF_DEFAULT_VALUE) && refTemp.split("_").length==3)
					{
						// Se recoge el numero que identifica a la referencia que es PENDIENTE_usuario_x capturamos posibles excepciones y seteamos 0 en defecto
						try
						{
							int value = Integer.parseInt(refTemp.split("_")[2]);
							referencias = Arrays.copyOf(referencias, referencias.length + 1);
							referencias[referencias.length - 1] = value;
						}
						catch(IndexOutOfBoundsException | NumberFormatException ex)
						{
							referencias = Arrays.copyOf(referencias, referencias.length + 1);
							referencias[referencias.length - 1] = 0;
						}
					}
				}
			}
		}
		int lastValue = 0;
		Arrays.sort(referencias);
		if(referencias.length>0)
		{
			lastValue = referencias[referencias.length - 1];
		}
		
		lastValue += 1;
		
		referenciaBulk = CPConstants.REF_DEFAULT_VALUE+"_"+this.encryptor.decrypt(usuario.getUsername())+"_"+String.valueOf(lastValue);
		return referenciaBulk;
	}
	
}
