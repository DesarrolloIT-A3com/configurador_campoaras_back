package es.aag.configurador.campoaras.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import es.aag.configurador.campoaras.dto.ResponseAcabado;
import es.aag.configurador.campoaras.dto.ResponseColor;
import es.aag.configurador.campoaras.dto.ResponseFrente;
import es.aag.configurador.campoaras.dto.ResponseProducto;
import es.aag.configurador.campoaras.dto.ResponseSerie;
import es.aag.configurador.campoaras.entities.Acabado;
import es.aag.configurador.campoaras.entities.Color;
import es.aag.configurador.campoaras.entities.Configuracion;
import es.aag.configurador.campoaras.entities.Frente;
import es.aag.configurador.campoaras.entities.Producto;
import es.aag.configurador.campoaras.entities.Serie;
import es.aag.configurador.campoaras.repositories.IAcabadoRepository;
import es.aag.configurador.campoaras.repositories.IColorRepository;
import es.aag.configurador.campoaras.repositories.IConfiguracionRepository;
import es.aag.configurador.campoaras.repositories.IFrenteRepository;
import es.aag.configurador.campoaras.repositories.IProductoRepository;
import es.aag.configurador.campoaras.repositories.ISerieRepository;
import es.aag.configurador.campoaras.utils.CPConstants;
import es.aag.configurador.campoaras.utils.CPException;
import es.aag.configurador.campoaras.utils.Validations;
/**
 * Servicio encargado de la gestión de productos y de contenido de la app
 * @author Pablo Ruiz (desarrolloit@a3com.es)
 * @version 1.0.0
 */
@Service
@Transactional
public class ManagmentService 
{
	private Logger log = LogManager.getLogger();
	
	@Autowired
	private EncryptorService encryptor;
	
	@Autowired
	private IProductoRepository productoRepo;
	
	@Autowired
	private ISerieRepository seriesRepo;
	
	@Autowired
	private IAcabadoRepository acabadoRepo;
	
	@Autowired
	private IColorRepository colorRepo;
	
	@Autowired
	private IFrenteRepository frenteRepo;
	
	@Autowired
	private IConfiguracionRepository configRepo;
	
	private final Validations validation;
	
	public ManagmentService()
	{
		this.validation = new Validations();
	}
	
	/**
	 * Metodo que devuelve, registra, actualiza y borra un producto dependiendo del endpointa a consultar, en caso de ser GET devuelve una lista de producto , si es otro método devuelve null
	 * @param body
	 * @param method
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 * @throws CPException
	 */
	public List<ResponseProducto> manageProduct(Producto body,String uuid,MultipartFile img,String method,String rol,String seguridad,String usrToken) throws CPException
	{
		
		this.validation.initialize(productoRepo, null, null, null, null, null, encryptor);
		
		List<ResponseProducto> response = null;
		
		
		switch(method)
		{
			case CPConstants.POST:
			{
				if(!this.validation.validateProduct(body))
				{
					log.warn("[AVISO] -- /products -- {} Ha introducido datos erroneos para subir un producto con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Producto producto = this.validation.findProducts(body.getNombre());
				
				if(producto != null)
				{
					log.warn("[AVISO] -- /product -- {} Ha intentado añadir un producto {} que ya existe con permiso de {} -- {}",usrToken,producto.getUuid(),rol,seguridad);
					throw new CPException(409,"Datos existentes");
				}
				
				producto = new Producto();
				producto.setUuid(UUID.randomUUID().toString());
				producto.setNombre(this.encryptor.encrypt(body.getNombre()));
				producto.setTipo(this.encryptor.encrypt(body.getTipo()));
				
				if(body.getCajon() != null)
				{
					if(!body.getCajon().isBlank())
					{
						producto.setCajon(this.encryptor.encrypt(body.getCajon()));
					}
				}
				
				// Transformacion del multipart a imagen 
				this.transformFile(img, producto.getUuid(), rol, "/products", usrToken, seguridad);
				
				log.info("[ADMIN] -- /products -- {} Ha añadido el producto {} a la base de datos con permiso de {} -- {}",usrToken,producto.getUuid(),rol,seguridad);
				
				this.productoRepo.save(producto);
				break;
			}
			case CPConstants.GET:
			{
				response = new LinkedList<ResponseProducto>();
				List<Producto> productos = this.productoRepo.findAll();
				
				for(Producto item:productos)
				{
					String uuidProducto = item.getUuid();
					String nombre = this.encryptor.decrypt(item.getNombre());
					String tipo = this.encryptor.decrypt(item.getTipo());
					String cajon = this.encryptor.decrypt(item.getCajon());
					byte [] imgB64 = this.loadImg(uuidProducto, "/products", rol, seguridad, usrToken);
					response.add(new ResponseProducto(uuidProducto,nombre,tipo,cajon,imgB64));
				}
				
				log.info("[ADMIN] -- /products -- {} Ha solicitado un listado de productos con permiso de {} -- {}",usrToken,rol,seguridad);		
				break;
			}
			case CPConstants.PATCH:
			{
				Optional<Producto> productoOpt = this.productoRepo.findById(uuid);
				
				if(!productoOpt.isPresent())
				{
					log.warn("[AVISO] -- /product -- {} Ha solicitado un producto que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				if(!this.validation.validateProduct(body))
				{
					log.warn("[AVISO] -- /product -- {} Ha introducido datos erroneos para subir un producto con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Producto producto = productoOpt.get();
				
				producto.setNombre(this.encryptor.encrypt(body.getNombre()));
				producto.setTipo(this.encryptor.encrypt(body.getTipo()));
				
				if(body.getCajon() != null)
				{
					if(!body.getCajon().isBlank())
					{
						producto.setCajon(this.encryptor.encrypt(body.getCajon()));
					}
				}
				
				this.transformFile(img, producto.getUuid(), rol, "/products", usrToken, seguridad);
				
				log.info("[ADMIN] -- /products -- {} Ha actualizado el producto {} a la base de datos con permiso de {} -- {}",usrToken,producto.getUuid(),rol,seguridad);
				
				this.productoRepo.save(producto);
				
				break;
			}
			case CPConstants.DELETE:
			{
				Optional<Producto> productoOpt = this.productoRepo.findById(uuid);
				
				if(!productoOpt.isPresent())
				{
					log.warn("[AVISO] -- /product -- {} Ha solicitado un producto que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Producto toDelete = productoOpt.get();
				
				log.info("[ADMIN] -- /product -- {} Ha eliminado el producto {} de la base de datos con permiso de {} -- {}",usrToken,toDelete.getUuid(),rol,seguridad);
				this.productoRepo.delete(toDelete);
				
				break;
			}
			default:
			{
				log.warn("[AVISO] -- /products -- {} Ha intentado acceder a la gestion de productos sin un metodo valido con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
		}
		
		this.validation.destroy();
		
		return response;
	}

	
	/**
	 * Metodo que devuelve, registra, actualiza y borra una serie dependiendo del endpoint a consultar, en caso de ser GET devuelve una lista de serie, si es otro método devuelve null,<br>NOTA: Se debe de referenciar a un producto para que se relacionen
	 * @param body
	 * @param method
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 * @throws CPException
	 */
	public List<ResponseSerie> manageSerie(Serie body,String uuid,MultipartFile img,String uuidProduct,String method,String rol,String seguridad,String usrToken) throws CPException
	{
		List<ResponseSerie> response = null;
		
		this.validation.initialize(null, null, null, null, seriesRepo, null, encryptor);
		
		switch(method)
		{
			case CPConstants.POST:
			{
				Optional<Producto> productOpt = this.productoRepo.findById(uuidProduct);
				
				if(!productOpt.isPresent())
				{
					log.warn("[AVISO] -- /series -- {} No ha introducido un producto para referenciar la serie a añadir con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Producto producto = productOpt.get();
				
				if(!this.validation.validateSerie(body))
				{
					log.warn("[AVISO] -- /series -- {} Ha introducido datos erroneos para subir una serie con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Serie serie = new Serie();
				serie.setUuid(UUID.randomUUID().toString());
				serie.setVariante(this.encryptor.encrypt(body.getVariante()));
				serie.setModulo(this.encryptor.encrypt(body.getModulo()));
				serie.setProducto(producto);
				
				if(body.getExtra() != null)
				{
					if(!body.getExtra().isBlank())
					{
						serie.setExtra(this.encryptor.encrypt(body.getExtra()));
					}
				}
				
				this.transformFile(img, serie.getUuid(), rol, "/series", usrToken, seguridad);
				
				log.info("[ADMIN] -- /series -- {} Ha añadido la serie {} a la base de datos con permiso de {} -- {}",usrToken,serie.getUuid(),rol,seguridad);
				
				this.seriesRepo.save(serie);
				break;
			}
			case CPConstants.GET:
			{
				response = new LinkedList<ResponseSerie>();
				List<Serie> series = this.seriesRepo.findAll();

				
				for(Serie serie:series)
				{
					
					if(uuidProduct == null || serie.getProducto().getUuid().equals(uuidProduct))
					{
						String variante = this.encryptor.decrypt(serie.getVariante());
						String modulo = this.encryptor.decrypt(serie.getModulo());
						String extra = this.encryptor.decrypt(serie.getExtra());
						
						
						// Hay que desencriptar el producto asociado
						Producto producto = serie.getProducto();
						String nombre = this.encryptor.decrypt(producto.getNombre());
						
						byte [] imgBytes = this.loadImg(serie.getUuid(), "/serie", rol, seguridad, usrToken);
						
						
						response.add(new ResponseSerie(serie.getUuid(), variante, modulo, extra, producto.getUuid(), nombre,imgBytes));
					}
					
				}
				
				log.info("[ADMIN] -- /series -- {} Ha solicitado un listado de series con permiso de {} -- {}",usrToken,rol,seguridad);
				break;
			}
			case CPConstants.PATCH:
			{
				
				Optional<Serie> serieOpt = this.seriesRepo.findById(uuid);
				
				if(!serieOpt.isPresent())
				{
					log.warn("[AVISO] -- /series -- {} Ha solicitado una serie que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Optional<Producto> productOpt = this.productoRepo.findById(uuidProduct);
				
				if(!productOpt.isPresent())
				{
					log.warn("[AVISO] -- /series -- {} No ha introducido un producto para referenciar la serie a añadir con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				if(!this.validation.validateSerie(body))
				{
					log.warn("[AVISO] -- /series -- {} Ha introducido datos erroneos para subir una serie con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Serie serie = serieOpt.get();
				
				serie.setVariante(this.encryptor.encrypt(body.getVariante()));
				serie.setModulo(this.encryptor.encrypt(body.getModulo()));
				serie.setProducto(productOpt.get());
				serie.setExtra(this.encryptor.encrypt(body.getExtra()));
				
				this.transformFile(img, serie.getUuid(), rol, "/series", usrToken, seguridad);

				log.info("[ADMIN] -- /series -- {} Ha actualizado la serie {} a la base de datos con permiso de {} -- {}",usrToken,serie.getUuid(),rol,seguridad);
				
				this.seriesRepo.save(serie);
				break;
			}
			case CPConstants.DELETE:
			{		
				Optional<Serie> serieOpt = this.seriesRepo.findById(uuid);
				
				if(!serieOpt.isPresent())
				{
					log.warn("[AVISO] -- /series -- {} Ha solicitado una serie que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Serie toDelete = serieOpt.get();
				
				log.info("[ADMIN] -- /series -- {} Ha eliminado la serie {} de la base de datos con permiso de {} -- {}",usrToken,toDelete.getUuid(),rol,seguridad);
				this.seriesRepo.delete(toDelete);
				break;
			}
			default:
			{
				log.warn("[AVISO] -- /series -- {} Ha intentado acceder a la gestion de series sin un metodo valido con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
		}
		
		this.validation.destroy();
		
		return response;
	}

	/**
	 * Metodo que devuelve, registra, actualiza y borra un acabado dependiendo del endpoint a consultar, en caso de ser GET devuelve una lista de acabados, si es otro método devuelve null este método se relaciona con la entidad Color
	 * @param body
	 * @param uuid
	 * @param method
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 * @throws CPException
	 */
	public List<ResponseAcabado> manageAcabado(ResponseAcabado body,String uuid,MultipartFile img,String method,String rol,String seguridad,String usrToken) throws CPException
	{
		this.validation.initialize(null, null, acabadoRepo, colorRepo, null, null, encryptor);
		
		List<ResponseAcabado> response = null;
		
		switch(method)
		{
			case CPConstants.POST:
			{
				if(!this.validation.validateAcabado(body))
				{
					log.warn("[AVISO] -- /acabados -- {} Ha introducido datos erroneos para subir un acabado con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Acabado acabado = this.validation.findAcabado(body.getNombre());
				
				if(acabado != null)
				{
					log.warn("[AVISO] -- /acabados -- {} Ha intentado añadir un acabado {} que ya existe con permiso de {} -- {}",usrToken,acabado.getUuid(),rol,seguridad);
					throw new CPException(409,"Datos existentes");
				}
				
				acabado = new Acabado();
				acabado.setUuid(UUID.randomUUID().toString());
				acabado.setNombre(this.encryptor.encrypt(body.getNombre()));
				
				String [] tipos = body.getTipos();
				tipos = tipos == null ? new String[0] : tipos; // Este ternario corrige tipos == null -> []
				String [] tiposEncrypt = new String[tipos.length];
				
				for(int i = 0;i<tipos.length;i++)
				{
					tiposEncrypt[i] = this.encryptor.encrypt(tipos[i]);
				}
				
				acabado.setTipos(tiposEncrypt);
				
				String [] bodyColores = body.getColores();
				
				if(bodyColores != null)
				{
					for(String item:bodyColores)
					{
						Color color = this.validation.findColor(item);
						if(color != null)
						{
							acabado.addColor(color);
						}
					}
				}
				
				this.transformFile(img, acabado.getUuid(), rol, "/acabados", usrToken, seguridad);
				
				log.info("[ADMIN] -- /acabados -- {} Ha añadido el producto {} a la base de datos con permiso de {} -- {}",usrToken,acabado.getUuid(),rol,seguridad);
				
				this.acabadoRepo.save(acabado);
				break;
			}
			case CPConstants.GET:
			{
				response = new LinkedList<ResponseAcabado>();
				List<Acabado> acabados = this.acabadoRepo.findAll();
				Optional<Configuracion> configOpt = Optional.empty();
					
				if(uuid!=null)
				{
					configOpt = this.configRepo.findById(uuid);
				}
				
				if(configOpt.isPresent())
				{
					acabados = new LinkedList<Acabado>();
					Configuracion config = configOpt.get();
					List<Map<String,Object>> armazon = config.getArmazon();
					
					for(Map<String,Object> item:armazon)
					{
						String nombre = (String) item.get("nombre");
						// Se desencripta debido a que el proceso de encriptación es distinto para cada valor y se encuentra por su valor bruto
						nombre = this.encryptor.decrypt(nombre);
						Acabado acabado = this.validation.findAcabado(nombre);
						
						if(acabado!=null)
						{
							acabados.add(acabado);
						}
					}
					
				}
				
				for(Acabado acabado:acabados)
				{
					String nombre = this.encryptor.decrypt(acabado.getNombre());
					
					String [] tipos = acabado.getTipos();
					String [] tiposEncrypt = new String[tipos.length];
					
					for(int i = 0;i<tipos.length;i++)
					{
						tiposEncrypt[i] = this.encryptor.decrypt(tipos[i]);
					}
					
					Set<Color> setColores = acabado.getColores();
					String [] colores = null;
					try
					{
						colores = new String[setColores.size()];
					
						int index = 0;
						for(Color color:setColores)
						{
							colores[index] = this.encryptor.decrypt(color.getNombre());
							index++;
						}
					}
					catch(NullPointerException ex)
					{
						colores = new String[0];
					}
					
					byte [] imgByte = this.loadImg(acabado.getUuid(), "/acabados", rol, seguridad, usrToken);
					
					response.add(new ResponseAcabado(acabado.getUuid(),nombre,tiposEncrypt,colores,imgByte));
				}
				
				log.info("[ADMIN] -- /acabados -- {} Ha solicitado un listado de acabados con permiso de {} -- {}",usrToken,rol,seguridad);		
				
				break;
			}
			case CPConstants.PATCH:
			{
				Optional<Acabado> acabadoOpt = this.acabadoRepo.findById(uuid);
				
				if(!acabadoOpt.isPresent())
				{
					log.warn("[AVISO] -- /acabados -- {} Ha solicitado un acabado que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				if(!this.validation.validateAcabado(body))
				{
					log.warn("[AVISO] -- /acabados -- {} Ha introducido datos erroneos para subir un acabado con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Acabado acabado = acabadoOpt.get();
				
				acabado.setNombre(this.encryptor.encrypt(body.getNombre()));

				String [] tipos = body.getTipos();
				tipos = tipos == null ? new String[0] : tipos; // Este ternario corrige tipos == null -> []
				String [] tiposEncrypt = new String[tipos.length];
				
				for(int i = 0;i<tipos.length;i++)
				{
					tiposEncrypt[i] = this.encryptor.encrypt(tipos[i]);
				}
				
				acabado.setTipos(tiposEncrypt);
								
				String [] bodyColores = body.getColores();
							
				if(bodyColores != null)
				{
					for(String item:bodyColores)
					{
						Color color = this.validation.findColor(item);
						if(color != null)
						{
							acabado.addColor(color);
						}
					}
				}
				
				this.transformFile(img, acabado.getUuid(), rol, "/acabados", usrToken, seguridad);
				
				log.info("[ADMIN] -- /acabados -- {} Ha actualizado el acabado {} a la base de datos con permiso de {} -- {}",usrToken,acabado.getUuid(),rol,seguridad);
				
				this.acabadoRepo.save(acabado);
				
				break;
			}
			case CPConstants.DELETE:
			{
				Optional<Acabado> acabadoOpt = this.acabadoRepo.findById(uuid);
				
				if(!acabadoOpt.isPresent())
				{
					log.warn("[AVISO] -- /acabados -- {} Ha solicitado un acabado que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Acabado toDelete = acabadoOpt.get();
				
				// Se crea una copia para evitar ConcurrentModificationException a la hora de borrar referencias de la entidad a borrar
				Set<Color> referencias = new HashSet<>(toDelete.getColores());
				
				if(referencias!=null)
				{
					for(Color color:referencias)
					{
						color.removeAcabado(toDelete);
					}
				}
				
				log.info("[ADMIN] -- /acabados -- {} Ha eliminado el acabado {} de la base de datos con permiso de {} -- {}",usrToken,toDelete.getUuid(),rol,seguridad);
				this.acabadoRepo.save(toDelete);
				this.acabadoRepo.delete(toDelete);
				this.acabadoRepo.flush();
				
				break;
			}
			default:
			{
				log.warn("[AVISO] -- /acabados -- {} Ha intentado acceder a la gestion de acabados sin un metodo valido con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
		}
		
		this.validation.destroy();
		
		return response;
	}
	
	/**
	 * Metodo que devuelve, registra, actualiza y borra un color dependiendo del endpoint a consultar, en caso de ser GET devuelve una lista de colores, si es otro método, devuelve null, este método se relaciona con la entidad Acabado
	 * @param body
	 * @param uuid
	 * @param method
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 * @throws CPException
	 */
	public List<ResponseColor> manageColor(ResponseColor body,String uuid,MultipartFile img,String method,String rol,String seguridad,String usrToken) throws CPException
	{
		
		this.validation.initialize(null, null, acabadoRepo, colorRepo, null, null, encryptor);
		
		List<ResponseColor> response = null;
		
		switch(method)
		{
			case CPConstants.POST:
			{
				if(!this.validation.validateColor(body))
				{
					log.warn("[AVISO] -- /colores -- {} Ha introducido datos erroneos para subir un color con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Color color = this.validation.findColor(body.getNombre());
				
				if(color != null)
				{
					log.warn("[AVISO] -- /colores -- {} Ha intentado añadir un color {} que ya existe con permiso de {} -- {}",usrToken,color.getUuid(),rol,seguridad);
					throw new CPException(409,"Datos existentes");
				}
				
				color = new Color();
				color.setUuid(UUID.randomUUID().toString());
				color.setNombre(this.encryptor.encrypt(body.getNombre()));
				
				this.colorRepo.save(color);
				this.colorRepo.flush();
				
				String [] bodyAcabados = body.getAcabados();
				
				if(bodyAcabados != null)
				{
					for(String item:bodyAcabados)
					{
						Acabado acabado = this.validation.findAcabado(item);
						
						if(acabado!=null)
						{
							color.addAcabado(acabado);
						}
					}
				};
				
				this.transformFile(img, color.getUuid(), "/colores", rol, usrToken, seguridad);
				
				log.info("[ADMIN] -- /colores -- {} Ha añadido el color {} a la base de datos con permiso de {} -- {}",usrToken,color.getUuid(),rol,seguridad);
				
				this.colorRepo.save(color);
				break;
			}
			case CPConstants.GET:
			{
				response = new LinkedList<ResponseColor>();
				List<Color> colores = this.colorRepo.findAll();
				
				boolean filter = false;
				
				for(Color color:colores)
				{
					filter = uuid == null;
					
					String uuidColor = color.getUuid();
					String nombre = this.encryptor.decrypt(color.getNombre());
					
					String [] acabados = null;
					
					// En caso de que acabados venga nulo se controla la excepcion
					try
					{
						Set<Acabado> acabadosSet = color.getAcabados();
						
						if(color.getAcabados().size()>0)
						{
							acabados = new String[acabadosSet.size()];
							int index = 0;
							for(Acabado acabado:acabadosSet)
							{
								acabados[index] = this.encryptor.decrypt(acabado.getNombre());
								index++;
								
								if(!filter)
								{
									filter = uuid.equals(acabado.getUuid());
								}
 							}
						}
					}
					catch(NullPointerException ex)
					{
						acabados = new String[0];
					}
					
					if(filter)
					{
						byte [] imgBytes = this.loadImg(color.getUuid(), "/colores", rol, seguridad, usrToken);
						
						response.add(new ResponseColor(uuidColor,nombre,acabados,imgBytes));
					}
					
					filter = false;
				}
				
				log.info("[ADMIN] -- /colores -- {} Ha solicitado un listado de colores con permiso de {} -- {}",usrToken,rol,seguridad);		
				break;
			}
			case CPConstants.PATCH:
			{
				Optional<Color> colorOpt = this.colorRepo.findById(uuid);
				
				if(!colorOpt.isPresent())
				{
					log.warn("[AVISO] -- /colores -- {} Ha solicitado un producto que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				if(!this.validation.validateColor(body))
				{
					log.warn("[AVISO] {} Ha introducido datos erroneos para subir un producto con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Color color = colorOpt.get();
				
				color.setNombre(this.encryptor.encrypt(body.getNombre()));
				
				String [] bodyAcabados = body.getAcabados();
				Set<Acabado> excluyentes = new HashSet<Acabado>();
				
				if(bodyAcabados!=null)
				{
					color.setAcabados(new HashSet<Acabado>());
					
					for(String item:bodyAcabados)
					{
						Acabado acabado = this.validation.findAcabado(item);
						
						if(acabado!=null)
						{
							color.addAcabado(acabado);
							excluyentes.add(acabado);
						}
					}
				}
				
				// Comprobamos que los acabados que posean el color actualizado mantengan la lista actualizada
				
				List<Acabado> acabados = this.acabadoRepo.findAll();
				
				for(Acabado acabado:acabados)
				{
					if(!excluyentes.contains(acabado) && acabado.getColores().contains(color))
					{
						acabado.removeColor(color);
					}
				}
				

				this.transformFile(img, color.getUuid(), "/colores", rol, usrToken, seguridad);
				
				log.info("[ADMIN] -- /colores -- {} Ha actualizado el color {} a la base de datos con permiso de {} -- {}",usrToken,color.getUuid(),rol,seguridad);
				
				this.colorRepo.save(color);
				
				break;
			}
			case CPConstants.DELETE:
			{
				Optional<Color> colorOpt = this.colorRepo.findById(uuid);
				
				if(!colorOpt.isPresent())
				{
					log.warn("[AVISO] -- /colores -- {} Ha solicitado un color que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Color toDelete = colorOpt.get();
				
				// Se crea una copia para evitar ConcurrentModificationException a la hora de borrar referencias de la entidad a borrar
				Set<Acabado> referencias = new HashSet<>(toDelete.getAcabados());
				
				if(referencias!=null)
				{
					for(Acabado acabado:referencias)
					{
						acabado.removeColor(toDelete);
					}
				}
				
				log.info("[ADMIN] -- /colores -- {} Ha eliminado el color {} de la base de datos con permiso de {} -- {}",usrToken,toDelete.getUuid(),rol,seguridad);
				this.colorRepo.save(toDelete);
				this.colorRepo.delete(toDelete);
				this.colorRepo.flush();
				break;
			}
			default:
			{
				log.warn("[AVISO] -- /colores -- {} Ha intentado acceder a la gestion de colores sin un metodo valido con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
		}
		
		this.validation.destroy();
		
		return response;
	}

	/**
	 * Metodo que devuelve, registra, actualiza y borra un producto dependiendo del endpointa a consultar, en caso de ser GET devuelve una lista de producto , si es otro método devuelve null
	 * @param body
	 * @param method
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @return
	 * @throws CPException
	 */
	public List<ResponseFrente> manageFrente(ResponseFrente body,String uuid,MultipartFile img,String method,String rol,String seguridad,String usrToken) throws CPException
	{
		
		this.validation.initialize(null, this.frenteRepo, this.acabadoRepo, null, null, null, encryptor);
		
		List<ResponseFrente> response = null;
		
		switch(method)
		{
			case CPConstants.POST:
			{
				if(!this.validation.validateFrente(body))
				{
					log.warn("[AVISO] -- /frentes -- {} Ha introducido datos erroneos para subir un frente con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Frente frente = this.validation.findFrentes(body.getNombre());
				
				if(frente != null)
				{
					log.warn("[AVISO] -- /frentes -- {} Ha intentado añadir un frente {} que ya existe con permiso de {} -- {}",usrToken,frente.getUuid(),rol,seguridad);
					throw new CPException(409,"Datos existentes");
				}
				
				String[] productos = body.getProductos();
				
				if(productos == null)
				{
					log.warn("[AVISO] -- /frentes -- {} Ha intentado añadir un frente sin asociar uno o varios productos con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				for(String item:productos)
				{
					Optional<Producto> productoOpt = this.productoRepo.findById(item);
					
					if(!productoOpt.isPresent())
					{
						log.warn("[AVISO] -- /frentes -- {} Ha intentado añadir un frente asociando un producto inexistente con permiso de {} -- {}",usrToken,rol,seguridad);
						throw new CPException(400,"Datos invalidos");
					}
				}
				
				frente = new Frente();
				frente.setUuid(UUID.randomUUID().toString());
				frente.setNombre(this.encryptor.encrypt(body.getNombre()));
				frente.setReferencia(this.encryptor.encrypt(body.getReferencia()));
				frente.setRegleta(body.isRegleta());
				frente.setTirador(body.isTirador());
				
				this.frenteRepo.save(frente);
				this.frenteRepo.flush();
				
				String [] bodyAcabados = body.getAcabados();
				
				if(bodyAcabados != null)
				{
					for(String item:bodyAcabados)
					{
						Acabado acabado = this.validation.findAcabado(item);
						
						if(acabado!=null)
						{
							frente.addAcabado(acabado);
						}
					}
				}
				
				String [] acabadosExtension = body.getAcabadosExtension();
				
				if(acabadosExtension != null)
				{
					for(String item:acabadosExtension)
					{
						Acabado acabado = this.validation.findAcabado(item);
						
						if(acabado != null)
						{
							frente.addAcabadoExtension(acabado);
						}
					}
				}
				
				for(String item:productos)
				{				
					Optional<Producto> productoOpt = this.productoRepo.findById(item);
					frente.addProducto(productoOpt.get());
				}
				
				this.transformFile(img, frente.getUuid(), rol, "/frentes", usrToken, seguridad);
				
				log.info("[ADMIN] -- /frentes -- {} Ha añadido el frente {} a la base de datos con permiso de {} -- {}",usrToken,frente.getUuid(),rol,seguridad);
				
				this.frenteRepo.save(frente);
				break;
			}
			case CPConstants.GET:
			{
				response = new LinkedList<ResponseFrente>();
				List<Frente> frentes = this.frenteRepo.findAll();
				
				boolean filter = false;
				
				for(Frente frente:frentes)
				{
					filter = uuid == null;
										
					String uuidFrente = frente.getUuid();
					String nombre = this.encryptor.decrypt(frente.getNombre());
					String referencia = this.encryptor.decrypt(frente.getReferencia());
					boolean regleta = frente.isRegleta();
					boolean tirador = frente.isTirador();
					
					String [] acabados = null;
					String [] acabadosExtension = null;
					
					// En caso de que acabados venga nulo se controla la excepcion
					try
					{
						Set<Acabado> acabadosSet = frente.getAcabados();
						
						if(frente.getAcabados().size()>0)
						{
							acabados = new String[acabadosSet.size()];
							int index = 0;
							for(Acabado acabado:acabadosSet)
							{
								acabados[index] = this.encryptor.decrypt(acabado.getNombre());
								index++;
 							}
						}
						
						acabadosSet = frente.getAcabadosExtension();
						
						if(frente.getAcabadosExtension().size()>0)
						{
							acabadosExtension = new String[acabadosSet.size()];
							int index = 0;
							for(Acabado acabado:acabadosSet)
							{
								acabadosExtension[index] = this.encryptor.decrypt(acabado.getNombre());
								index++;
 							}
						}
						
					}
					catch(NullPointerException ex)
					{
						if(acabados == null)
						{
							acabados = new String[0];
						}
						
						if(acabadosExtension == null)
						{
							acabadosExtension = new String[0];
						}
						
					}
					
					Set<Producto> productosSet = frente.getProductoFrente();
					
					if(productosSet.isEmpty())
					{
						// En caso de que un frente no tenga producto se salta la iteracción paar evitar errores
						continue;
					}
					
					
					String [] productos = new String[productosSet.size()];
					int index = 0;
					
					for(Producto item:productosSet)
					{
						productos[index] = this.encryptor.decrypt(item.getNombre());
						index++;
						if(!filter)
						{
							filter = uuid.equals(item.getUuid());
						}
					}
					
					if(filter)
					{
						byte [] imgBytes = this.loadImg(uuidFrente, "/frentes", rol, seguridad, usrToken);
						 
						response.add(new ResponseFrente(uuidFrente, nombre, referencia, regleta, tirador,acabados,acabadosExtension,productos,imgBytes));
					}
					
					filter = false;
					
					
				}
				
				log.info("[ADMIN] -- /frentes -- {} Ha solicitado un listado de frentes con permiso de {} -- {}",usrToken,rol,seguridad);		
				break;
			}
			case CPConstants.PATCH:
			{
				Optional<Frente> frenteOpt = this.frenteRepo.findById(uuid);
				
				if(!frenteOpt.isPresent())
				{
					log.warn("[AVISO] -- /frentes -- {} Ha solicitado un frente que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				if(!this.validation.validateFrente(body))
				{
					log.warn("[AVISO] -- /frentes -- {} Ha introducido datos erroneos para subir un frente con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				Frente frente = frenteOpt.get();
				
				frente.setNombre(this.encryptor.encrypt(body.getNombre()));
				frente.setReferencia(this.encryptor.encrypt(body.getReferencia()));
				
				String [] bodyAcabados = body.getAcabados();
				String [] acabadosExtension = body.getAcabadosExtension();
				Set<Acabado> excluyentes = new HashSet<Acabado>();
			
				if(bodyAcabados != null)
				{
					for(String item:bodyAcabados)
					{
						Acabado acabado = this.validation.findAcabado(item);
						
						if(acabado!=null)
						{
							frente.addAcabado(acabado);
							excluyentes.add(acabado);
						}
					}
				}
				
				// Comprobamos que los acabados que posean el frente actualizado mantengan la lista actualizada
				
				List<Acabado> acabados = this.acabadoRepo.findAll();
				
				for(Acabado acabado:acabados)
				{
					if(!excluyentes.contains(acabado) && acabado.getFrentes().contains(frente))
					{
						acabado.removeFrente(frente);
					}
				}
				
				excluyentes = new HashSet<Acabado>();
				
				if(acabadosExtension != null)
				{
					for(String item:acabadosExtension)
					{
						Acabado acabado = this.validation.findAcabado(item);
						
						if(acabado!=null)
						{
							frente.addAcabadoExtension(acabado);
							excluyentes.add(acabado);
						}
					}
				}
				
				// Realizamos lo mismo para los acabados de las regletas
				
				for(Acabado acabado:acabados)
				{
					if(!excluyentes.contains(acabado) && acabado.getFrentesExtension().contains(frente))
					{
						acabado.removeFrenteExtension(frente);
					}
				}
				
				String[] productos = body.getProductos();
				
				if(productos == null)
				{
					log.warn("[AVISO] -- /frentes -- {} Ha intentado añadir un frente sin asociar uno o varios productos con permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(400,"Datos invalidos");
				}
				
				frente.setProductoFrente(new HashSet<Producto>());
				
				Set<Producto> prodExcluyentes = new HashSet<Producto>();
				
				for(String item:productos)
				{
					Optional<Producto> productoOpt = this.productoRepo.findById(item);
					
					if(!productoOpt.isPresent())
					{
						log.warn("[AVISO] -- /frentes -- {} Ha intentado añadir un frente asociando un producto inexistente con permiso de {} -- {}",usrToken,rol,seguridad);
						throw new CPException(400,"Datos invalidos");
					}
					
					frente.addProducto(productoOpt.get());
				}
				
				// Realizamos la comprobación anterior pero con los productos
				
				List<Producto> productosList = this.productoRepo.findAll();
				
				for(Producto producto:productosList)
				{
					if(!prodExcluyentes.add(producto) && producto.getFrentesProductos().contains(frente))
					{
						producto.removeProducto(frente);
					}
				}
				
				this.transformFile(img, frente.getUuid(), rol, "/frentes", usrToken, seguridad);
				
				log.info("[ADMIN] -- /frentes -- {} Ha actualizado el frente {} a la base de datos con permiso de {} -- {}",usrToken,frente.getUuid(),rol,seguridad);
				
				this.frenteRepo.save(frente);
				
				break;
			}
			case CPConstants.DELETE:
			{
				Optional<Frente> frenteOpt = this.frenteRepo.findById(uuid);
				
				if(!frenteOpt.isPresent())
				{
					log.warn("[AVISO] -- /frentes -- {} Ha solicitado un producto que no existe con un permiso de {} -- {}",usrToken,rol,seguridad);
					throw new CPException(404,"Dato o datos no encontrados");
				}
				
				Frente toDelete = frenteOpt.get();
				
				// Se crea una copia para evitar ConcurrentModificationException a la hora de borrar referencias de la entidad a borrar
				Set<Acabado> referencias = new HashSet<>(toDelete.getAcabados());
				
				if(referencias!=null)
				{
					for(Acabado acabado:referencias)
					{
						acabado.removeFrente(toDelete);
					}
				}
				
				referencias = new HashSet<>(toDelete.getAcabadosExtension());
				
				if(referencias!=null)
				{
					for(Acabado acabado:referencias)
					{
						acabado.removeFrenteExtension(toDelete);
					}
				}
				
				Set<Producto> referenciasProd = new HashSet<>(toDelete.getProductoFrente());
				
				if(referenciasProd!=null)
				{
					for(Producto producto:referenciasProd)
					{
						producto.removeProducto(toDelete);
					}
				}
				
				log.info("[ADMIN] -- /frentes -- {} Ha eliminado el frente {} de la base de datos con permiso de {} -- {}",usrToken,toDelete.getUuid(),rol,seguridad);
				this.frenteRepo.save(toDelete);
				this.frenteRepo.delete(toDelete);
				this.frenteRepo.flush();
				break;
			}
			default:
			{
				log.warn("[AVISO] -- /frentes -- {} Ha intentado acceder a la gestion de frentes sin un metodo valido con un permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(403,"No tienes permiso");
			}
		}
		
		this.validation.destroy();
		
		return response;
	}
	
	/**
	 * Metodo TEMPORAL de desarrollo para lanzar datos de prueba, en produccion se borrará por lo que solo será visible en la sección de desarrollo
	 * @param rol
	 * @param seguridad
	 * @param usrToken
	 * @throws CPException
	 */
	public void launchData(String rol, String seguridad,String usrToken) throws CPException
	{
		if(!rol.equals(CPConstants.SUPADMIN_ROLE))
		{
			log.warn("[AVISO] -- /upt-user -- {} Ha intentado obtener informacion de los usuarios con un permiso de {} -- {}",usrToken,rol,seguridad);
			throw new CPException(403,"No tienes permiso");
		}
		
		// PRODUCTOS
		
		String uuid = UUID.randomUUID().toString();
		String nombre = this.encryptor.encrypt("Aqua");
		String tipo = this.encryptor.encrypt("Mueble");
		String cajon = this.encryptor.encrypt("Hettich");
		Producto aqua = new Producto(uuid,nombre,tipo,cajon,new HashSet<Serie>(),new HashSet<Frente>());
		
		log.info("[ADMIN] -- /load -- {} Ha creado el producto AQUA para pruebas con permiso de {} -- {}",usrToken,rol,seguridad);
		this.productoRepo.save(aqua);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Drop");
		tipo = this.encryptor.encrypt("Mueble");
		cajon = this.encryptor.encrypt("Hettich");
		Producto drop = new Producto(uuid,nombre,tipo,cajon,new HashSet<Serie>(),new HashSet<Frente>());
		
		log.info("[ADMIN] -- /load -- {} Ha creado el producto DROP para pruebas con permiso de {} -- {}",usrToken,rol,seguridad);
		this.productoRepo.save(drop);
		
		// SERIES
		
		uuid = UUID.randomUUID().toString();
		String variante = this.encryptor.encrypt("Portalavabo 1 cajón");
		String modulo = this.encryptor.encrypt("Modular suspendido altura 30");
		
		Serie aquaSerie1 = new Serie(uuid,variante,modulo,null,aqua,new HashSet<Configuracion>());
		Serie dropSerie1 = new Serie(UUID.randomUUID().toString(),variante,modulo,null,drop,new HashSet<Configuracion>());
		
		uuid = UUID.randomUUID().toString();
	    variante = this.encryptor.encrypt("Portalavabo 2 cajones horizontales");
		modulo = this.encryptor.encrypt("Modular suspendido altura 30");
		String extra = this.encryptor.encrypt("Salvasifón centrado");
		
		Serie aquaSerie2 = new Serie(uuid,variante,modulo,extra,aqua,new HashSet<Configuracion>());
		Serie dropSerie2 = new Serie(UUID.randomUUID().toString(),variante,modulo,extra,drop,new HashSet<Configuracion>());
		
		log.info("[ADMIN] -- /load -- {} Ha creado series para los productos AQUA y DROP  para pruebas con permiso de {} -- {}",usrToken,rol,seguridad);
		this.seriesRepo.save(aquaSerie1);
		this.seriesRepo.save(dropSerie1);
		this.seriesRepo.save(aquaSerie2);
		this.seriesRepo.save(dropSerie2);
		
		// ACABADOS
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Melamina");
		
		Acabado acabado1 = new Acabado();
		acabado1.setUuid(uuid);
		acabado1.setNombre(nombre);
		acabado1.setTipos(new String [0]);

		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Laminado");
		String [] tipos = {this.encryptor.encrypt("Lacado MATE")};
		
		Acabado acabado2 = new Acabado();
		acabado2.setUuid(uuid);
		acabado2.setNombre(nombre);
		acabado2.setTipos(tipos);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Marmoleado");
		tipos[0] = this.encryptor.encrypt("Lacado BRILLO");
		
		Acabado acabado3 = new Acabado();
		acabado3.setUuid(uuid);
		acabado3.setNombre(nombre);
		acabado3.setTipos(tipos);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Roble Natural");
		tipos[0] = this.encryptor.encrypt("Chapa Natural de madera");
		
		Acabado acabado4 = new Acabado();
		acabado4.setUuid(uuid);
		acabado4.setNombre(nombre);
		acabado4.setTipos(tipos);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Roble Seda");
		tipos[0] = this.encryptor.encrypt("Chapa Natural de madera");
		
		Acabado acabado5 = new Acabado();
		acabado5.setUuid(uuid);
		acabado5.setNombre(nombre);
		acabado5.setTipos(tipos);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Roble Tinte");
		tipos[0] = this.encryptor.encrypt("Chapa Natural de madera");
		
		Acabado acabado6 = new Acabado();
		acabado6.setUuid(uuid);
		acabado6.setNombre(nombre);
		acabado6.setTipos(tipos);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Nogal Seda");
		tipos[0] = this.encryptor.encrypt("Chapa Natural de madera");
		
		Acabado acabado7 = new Acabado();
		acabado7.setUuid(uuid);
		acabado7.setNombre(nombre);
		acabado7.setTipos(tipos);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Frente Fresado");
		tipos[0] = this.encryptor.encrypt("Especial");
		
		Acabado acabado8 = new Acabado();
		acabado8.setUuid(uuid);
		acabado8.setNombre(nombre);
		acabado8.setTipos(tipos);
		
		List<Acabado> acabados = List.of(acabado1,acabado2,acabado3,acabado4,acabado5,acabado6,acabado7,acabado8);
		log.info("[ADMIN] -- /load -- {} Ha creado acabados para pruebas con permiso de {} -- {}",usrToken,rol,seguridad);
	
		this.acabadoRepo.saveAll(acabados);
		
		// COLORES
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Arce");
		Color color1 = new Color();
		color1.setUuid(uuid);
		color1.setNombre(nombre);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Rojo");
		Color color2 = new Color();
		color2.setUuid(uuid);
		color2.setNombre(nombre);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Blanco");
		Color color3 = new Color();
		color3.setUuid(uuid);
		color3.setNombre(nombre);
		
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Blanco RAL 9001");
		Color color4 = new Color();
		color4.setUuid(uuid);
		color4.setNombre(nombre);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Blanco mármol");
		Color color5 = new Color();
		color5.setUuid(uuid);
		color5.setNombre(nombre);
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Gris mármol");
		Color color6 = new Color();
		color6.setUuid(uuid);
		color6.setNombre(nombre);
		
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Marrón");
		Color color7 = new Color();
		color7.setUuid(uuid);
		color7.setNombre(nombre);
		
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Roble");
		Color color8 = new Color();
		color8.setUuid(uuid);
		color8.setNombre(nombre);
		
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Nogal");
		Color color9 = new Color();
		color9.setUuid(uuid);
		color9.setNombre(nombre);
		
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Oceano");
		Color color10 = new Color();
		color10.setUuid(uuid);
		color10.setNombre(nombre);
		
		List<Color> colores = List.of(color1,color2,color3,color4,color5,color6,color7,color8,color9,color10);
		log.info("[ADMIN] -- /load -- {} Ha creado acabados para pruebas con permiso de {} -- {}",usrToken,rol,seguridad);
		
		this.colorRepo.saveAll(colores);
		
		// FRENTES
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Inglete");
		String referencia = this.encryptor.encrypt("IN");
		Frente frente1 = new Frente();
		frente1.setUuid(uuid);
		frente1.setNombre(nombre);
		frente1.setReferencia(referencia);
		frente1.setRegleta(true);
		frente1.setTirador(false);
		frente1.addProducto(aqua);
		frente1.addProducto(drop);
		
		
		
		uuid = UUID.randomUUID().toString();
		nombre = this.encryptor.encrypt("Embutido Cromo");
		referencia = this.encryptor.encrypt("EC");
		Frente frente2 = new Frente();
		frente2.setUuid(uuid);
		frente2.setNombre(nombre);
		frente2.setReferencia(referencia);
		frente2.setRegleta(true);
		frente2.setTirador(false);		
		frente2.addProducto(aqua);
		frente2.addProducto(drop);
		log.info("[ADMIN] -- /load -- {} Ha creado frentes para pruebas con permiso de {} -- {}",usrToken,rol,seguridad);
		this.frenteRepo.save(frente1);
		this.frenteRepo.save(frente2);
		
		this.productoRepo.save(aqua);
		this.productoRepo.save(drop);
		
		// CONFIGURACIONES
		referencia = "2020";
		float fondo = 37.8f;
		float ancho = 50f;
		float alto = 30f;
		
		Map<String,Object> melamina = new HashMap<String, Object>();
		melamina.put("nombre", this.encryptor.encrypt("Melamina"));
		melamina.put("precio", 218.60f);
		Map<String,Object> laminado = new HashMap<String, Object>();
		laminado.put("nombre", this.encryptor.encrypt("Laminado"));
		laminado.put("precio", 233.90f);
		Map<String,Object> marmoleado = new HashMap<String, Object>();
		marmoleado.put("nombre", this.encryptor.encrypt("Marmoleado"));
		marmoleado.put("precio", 258.80f);
		Map<String,Object> rNatural = new HashMap<String, Object>();
		rNatural.put("nombre", this.encryptor.encrypt("Roble Natural"));
		rNatural.put("precio", 281.25f);
		Map<String,Object> rSeda = new HashMap<String, Object>();
		rSeda.put("nombre", this.encryptor.encrypt("Roble Seda"));
		rSeda.put("precio", 288.45f);
		Map<String,Object> rTinte = new HashMap<String, Object>();
		rTinte.put("nombre", this.encryptor.encrypt("Roble Tinte"));
		rTinte.put("precio", 303.40f);
		Map<String,Object> nSeda = new HashMap<String, Object>();
		nSeda.put("nombre", this.encryptor.encrypt("Nogal Seda"));
	    nSeda.put("precio", 296.95f);
		Map<String,Object> fFresado = new HashMap<String, Object>();
		fFresado.put("nombre", this.encryptor.encrypt("Frente Fresado"));
		fFresado.put("precio", 40.0f);
		
		List<Map<String,Object>> armazon = List.of(melamina,laminado,marmoleado,rNatural,rSeda,rTinte,nSeda,fFresado);
		
		Configuracion configuracion = new Configuracion();
		configuracion.setReferencia(referencia);
		configuracion.setFondo(fondo);
		configuracion.setAncho(ancho);
		configuracion.setAlto(alto);
		configuracion.setAltoMax(50);
		configuracion.setFondoMin(46);
		configuracion.setFondoMax(50);
		configuracion.setPrecioMedidaFondoEsp(30);
		configuracion.setPrecioMedidaAnchoEsp(50);
		configuracion.setPrecioMedidaAltoEsp(50);
		configuracion.setArmazon(armazon);
		configuracion.setSerie(aquaSerie1);
		
		aquaSerie1.addConfiguracion(configuracion);
		
		referencia = "1701";
		fondo = 44.8f;
		ancho = 70f;
		alto = 30f;
		
		melamina = new HashMap<String, Object>();
		melamina.put("nombre",  this.encryptor.encrypt("Melamina"));
		melamina.put("precio", 232.10f);
		laminado = new HashMap<String, Object>();
		laminado.put("nombre",  this.encryptor.encrypt("Laminado"));
		laminado.put("precio", 248.40f);
		marmoleado = new HashMap<String, Object>();
		marmoleado.put("nombre",  this.encryptor.encrypt("Marmoleado"));
		marmoleado.put("precio", 277.0f);
		rNatural = new HashMap<String, Object>();
		rNatural.put("nombre",  this.encryptor.encrypt("Roble Natural"));
		rNatural.put("precio", 302.40f);
		rSeda = new HashMap<String, Object>();
		rSeda.put("nombre",  this.encryptor.encrypt("Roble Seda"));
		rSeda.put("precio", 310.60f);
		rTinte = new HashMap<String, Object>();
		rTinte.put("nombre",  this.encryptor.encrypt("Roble Tinte"));
		rTinte.put("precio", 326.30f);
		nSeda = new HashMap<String, Object>();
		nSeda.put("nombre",  this.encryptor.encrypt("Nogal Seda"));
		nSeda.put("precio", 320.35f);
		fFresado = new HashMap<String, Object>();
		fFresado.put("nombre",  this.encryptor.encrypt("Frente Fresado"));
		fFresado.put("precio", 40.0f);
		
		armazon = List.of(melamina,laminado,marmoleado,rNatural,rSeda,rTinte,nSeda,fFresado);
		
		Configuracion configuracion1 = new Configuracion();
		configuracion1.setReferencia(referencia);
		configuracion1.setFondo(fondo);
		configuracion1.setAncho(ancho);
		configuracion1.setAlto(alto);
		configuracion1.setAltoMax(50);
		configuracion1.setFondoMin(46);
		configuracion1.setFondoMax(50);
		configuracion1.setPrecioMedidaFondoEsp(30);
		configuracion1.setPrecioMedidaAnchoEsp(50);
		configuracion1.setPrecioMedidaAltoEsp(50);
		configuracion1.setArmazon(armazon);
		configuracion1.setSerie(aquaSerie1);
		
		aquaSerie1.addConfiguracion(configuracion1);
		
		referencia = "3291";
		fondo = 44.8f;
		ancho = 50f;
		alto = 30f;
		
		melamina = new HashMap<String, Object>();
		melamina.put("nombre",  this.encryptor.encrypt("Melamina"));
		melamina.put("precio", 218.60f);
		laminado = new HashMap<String, Object>();
		laminado.put("nombre",  this.encryptor.encrypt("Laminado"));
		laminado.put("precio", 233.90f);
		marmoleado = new HashMap<String, Object>();
		marmoleado.put("nombre",  this.encryptor.encrypt("Marmoleado"));
		marmoleado.put("precio", 269.50f);
		rNatural = new HashMap<String, Object>();
		rNatural.put("nombre",  this.encryptor.encrypt("Roble Natural"));
		rNatural.put("precio", 281.25f);
		rSeda = new HashMap<String, Object>();
		rSeda.put("nombre",  this.encryptor.encrypt("Roble Seda"));
		rSeda.put("precio", 288.45f);
		rTinte = new HashMap<String, Object>();
		rTinte.put("nombre",  this.encryptor.encrypt("Roble Tinte"));
		rTinte.put("precio", 303.45f);
		nSeda = new HashMap<String, Object>();
		nSeda.put("nombre",  this.encryptor.encrypt("Nogal Seda"));
		nSeda.put("precio", 296.95f);
		fFresado = new HashMap<String, Object>();
		fFresado.put("nombre",  this.encryptor.encrypt("Frente Fresado"));
		fFresado.put("precio", 40.0f);
		
		armazon = List.of(melamina,laminado,marmoleado,rNatural,rSeda,rTinte,nSeda,fFresado);
		
		Configuracion configuracion2 = new Configuracion();
		configuracion2.setReferencia(referencia);
		configuracion2.setFondo(fondo);
		configuracion2.setAncho(ancho);
		configuracion2.setAlto(alto);
		configuracion2.setAltoMax(50);
		configuracion2.setFondoMin(46);
		configuracion2.setFondoMax(50);
		configuracion2.setPrecioMedidaFondoEsp(30);
		configuracion2.setPrecioMedidaAnchoEsp(50);
		configuracion2.setPrecioMedidaAltoEsp(50);
		configuracion2.setArmazon(armazon);
		configuracion2.setSerie(dropSerie1);
		
		dropSerie1.addConfiguracion(configuracion2);
		
		referencia = "3252";
		fondo = 37.8f;
		ancho = 120f;
		alto = 30f;
		
		melamina = new HashMap<String, Object>();
		melamina.put("nombre",  this.encryptor.encrypt("Melamina"));
		melamina.put("precio", 368.60f);
		laminado = new HashMap<String, Object>();
		laminado.put("nombre",  this.encryptor.encrypt("Laminado"));
		laminado.put("precio", 396.10f);
		marmoleado = new HashMap<String, Object>();
		marmoleado.put("nombre",  this.encryptor.encrypt("Marmoleado"));
		marmoleado.put("precio", 434.15f);
		rNatural = new HashMap<String, Object>();
		rNatural.put("nombre",  this.encryptor.encrypt("Roble Natural"));
		rNatural.put("precio", 475.60f);
		rSeda = new HashMap<String, Object>();
		rSeda.put("nombre",  this.encryptor.encrypt("Roble Seda"));
		rSeda.put("precio", 486.50f);
		rTinte = new HashMap<String, Object>();
		rTinte.put("nombre",  this.encryptor.encrypt("Roble Tinte"));
		rTinte.put("precio", 507.30f);
		nSeda = new HashMap<String, Object>();
		nSeda.put("nombre",  this.encryptor.encrypt("Nogal Seda"));
		nSeda.put("precio", 499.55f);
		fFresado = new HashMap<String, Object>();
		fFresado.put("nombre",  this.encryptor.encrypt("Frente Fresado"));
		fFresado.put("precio", 70.0f);
		
		armazon = List.of(melamina,laminado,marmoleado,rNatural,rSeda,rTinte,nSeda,fFresado);
		
		Configuracion configuracion3 = new Configuracion();
		configuracion3.setReferencia(referencia);
		configuracion3.setFondo(fondo);
		configuracion3.setAncho(ancho);
		configuracion3.setAlto(alto);
		configuracion3.setAltoMax(50);
		configuracion3.setFondoMin(46);
		configuracion3.setFondoMax(50);
		configuracion3.setPrecioMedidaFondoEsp(30);
		configuracion3.setPrecioMedidaAnchoEsp(50);
		configuracion3.setPrecioMedidaAltoEsp(50);
		configuracion3.setArmazon(armazon);
		configuracion3.setSerie(dropSerie2);
		
		dropSerie2.addConfiguracion(configuracion3);
		
		referencia = "3253";
		fondo = 37.8f;
		ancho = 140f;
		alto = 30f;
		
		melamina = new HashMap<String, Object>();
		melamina.put("nombre",  this.encryptor.encrypt("Melamina"));
		melamina.put("precio", 433.15f);
		laminado = new HashMap<String, Object>();
		laminado.put("nombre",  this.encryptor.encrypt("Laminado"));
		laminado.put("precio", 463.30f);
		marmoleado = new HashMap<String, Object>();
		marmoleado.put("nombre",  this.encryptor.encrypt("Marmoleado"));
		marmoleado.put("precio", 503.05f);
		rNatural = new HashMap<String, Object>();
		rNatural.put("nombre",  this.encryptor.encrypt("Roble Natural"));
		rNatural.put("precio", 548.20f);
		rSeda = new HashMap<String, Object>();
		rSeda.put("nombre",  this.encryptor.encrypt("Roble Seda"));
		rSeda.put("precio", 560.25f);
		rTinte = new HashMap<String, Object>();
		rTinte.put("nombre",  this.encryptor.encrypt("Roble Tinte"));
		rTinte.put("precio", 581.80f);
		nSeda = new HashMap<String, Object>();
		nSeda.put("nombre",  this.encryptor.encrypt("Nogal Seda"));
		nSeda.put("precio", 574.60f);
		fFresado = new HashMap<String, Object>();
		fFresado.put("nombre",  this.encryptor.encrypt("Frente Fresado"));
		fFresado.put("precio", 70.0f);
		
		armazon = List.of(melamina,laminado,marmoleado,rNatural,rSeda,rTinte,nSeda,fFresado);
		
		Configuracion configuracion4 = new Configuracion();
		configuracion4.setReferencia(referencia);
		configuracion4.setFondo(fondo);
		configuracion4.setAncho(ancho);
		configuracion4.setAlto(alto);
		configuracion4.setAltoMax(50);
		configuracion4.setFondoMin(46);
		configuracion4.setFondoMax(50);
		configuracion4.setPrecioMedidaFondoEsp(30);
		configuracion4.setPrecioMedidaAnchoEsp(50);
		configuracion4.setPrecioMedidaAltoEsp(50);
		configuracion4.setArmazon(armazon);
		configuracion4.setSerie(dropSerie2);
		
		dropSerie2.addConfiguracion(configuracion4);
		
		referencia = "1706";
		fondo = 37.8f;
		ancho = 70;
		alto = 30f;
		
		melamina = new HashMap<String, Object>();
		melamina.put("nombre",  this.encryptor.encrypt("Melamina"));
		melamina.put("precio", 433.15f);
		laminado = new HashMap<String, Object>();
		laminado.put("nombre",  this.encryptor.encrypt("Laminado"));
		laminado.put("precio", 463.30f);
		marmoleado = new HashMap<String, Object>();
		marmoleado.put("nombre",  this.encryptor.encrypt("Marmoleado"));
		marmoleado.put("precio", 503.05f);
		rNatural = new HashMap<String, Object>();
		rNatural.put("nombre",  this.encryptor.encrypt("Roble Natural"));
		rNatural.put("precio", 548.20f);
		rSeda = new HashMap<String, Object>();
		rSeda.put("nombre",  this.encryptor.encrypt("Roble Seda"));
		rSeda.put("precio", 560.25f);
		rTinte = new HashMap<String, Object>();
		rTinte.put("nombre",  this.encryptor.encrypt("Roble Tinte"));
		rTinte.put("precio", 581.80f);
		nSeda = new HashMap<String, Object>();
		nSeda.put("nombre",  this.encryptor.encrypt("Nogal Seda"));
		nSeda.put("precio", 574.60f);
		fFresado = new HashMap<String, Object>();
		fFresado.put("nombre",  this.encryptor.encrypt("Frente Fresado"));
		fFresado.put("precio", 70.0f);
		
		armazon = List.of(melamina,laminado,marmoleado,rNatural,rSeda,rTinte,nSeda,fFresado);
		
		Configuracion configuracion5 = new Configuracion();
		configuracion5.setReferencia(referencia);
		configuracion5.setFondo(fondo);
		configuracion5.setAncho(ancho);
		configuracion5.setAlto(alto);
		configuracion5.setAltoMax(50);
		configuracion5.setFondoMin(46);
		configuracion5.setFondoMax(50);
		configuracion5.setPrecioMedidaFondoEsp(30);
		configuracion5.setPrecioMedidaAnchoEsp(50);
		configuracion5.setPrecioMedidaAltoEsp(50);
		configuracion5.setArmazon(armazon);
		configuracion5.setSerie(aquaSerie1);
		
		aquaSerie1.addConfiguracion(configuracion5);
		
		
		List<Configuracion> configuraciones = List.of(configuracion,configuracion1,configuracion2,configuracion3,configuracion4,configuracion5);
		List<Serie> series = List.of(aquaSerie1,aquaSerie2,dropSerie1,dropSerie2);
		log.info("[AVISO] -- /load -- {} Ha creado configuraciones para los productos AQUA y DROP con permiso de {} -- {}",usrToken,rol,seguridad);
		
		this.configRepo.saveAll(configuraciones);
		this.seriesRepo.saveAll(series);
		this.seriesRepo.flush();
		
		color1.addAcabado(acabado1);
		color2.addAcabado(acabado1);
		color3.addAcabado(acabado1);
		color3.addAcabado(acabado2);
		color4.addAcabado(acabado2);
		color5.addAcabado(acabado3);
		color6.addAcabado(acabado3);
		color7.addAcabado(acabado1);
		color7.addAcabado(acabado4);
		color7.addAcabado(acabado5);
		color7.addAcabado(acabado6);
		color8.addAcabado(acabado1);
		color8.addAcabado(acabado4);
		color8.addAcabado(acabado5);
		color8.addAcabado(acabado6);
		color9.addAcabado(acabado7);
		color10.addAcabado(acabado8);

		
		
		acabados = List.of(acabado1,acabado2,acabado3,acabado4,acabado5,acabado6,acabado7,acabado8);
		colores = List.of(color1,color2,color3,color4,color5,color6,color7,color8,color9,color10);
		
		log.info("[ADMIN] -- /load -- {} Ha gestionado las relaciones de acabados y colores con permiso de {} -- {}",usrToken,rol,seguridad);
		this.acabadoRepo.saveAll(acabados);
		this.colorRepo.saveAll(colores);
		
		frente1.addAcabado(acabado1);
		frente1.addAcabado(acabado2);
		frente1.addAcabado(acabado3);
		frente1.addAcabado(acabado4);
		frente1.addAcabado(acabado5);
		frente1.addAcabado(acabado6);
		frente1.addAcabado(acabado7);
		frente1.addAcabado(acabado8);
		frente2.addAcabado(acabado1);
		frente2.addAcabado(acabado2);
		frente2.addAcabado(acabado3);
		frente2.addAcabado(acabado4);
		frente2.addAcabado(acabado5);
		frente2.addAcabado(acabado6);
		frente2.addAcabado(acabado7);
		frente2.addAcabado(acabado8);
		
		frente1.addAcabadoExtension(acabado1);
		frente1.addAcabadoExtension(acabado2);
		frente1.addAcabadoExtension(acabado3);
		frente1.addAcabadoExtension(acabado4);
		frente1.addAcabadoExtension(acabado5);
		frente1.addAcabadoExtension(acabado6);
		frente1.addAcabadoExtension(acabado7);
		frente1.addAcabadoExtension(acabado8);
		frente2.addAcabadoExtension(acabado1);
		frente2.addAcabadoExtension(acabado2);
		frente2.addAcabadoExtension(acabado3);
		frente2.addAcabadoExtension(acabado4);
		frente2.addAcabadoExtension(acabado5);
		frente2.addAcabadoExtension(acabado6);
		frente2.addAcabadoExtension(acabado7);
		frente2.addAcabadoExtension(acabado8);
		
		acabados = List.of(acabado1,acabado2,acabado3,acabado4,acabado5,acabado6,acabado7,acabado8);
		
		log.info("[ADMIN] -- /load -- {} Ha gestionado las relaciones de acabados y frentes con permiso de {} -- {}",usrToken,rol,seguridad);
		this.acabadoRepo.saveAll(acabados);
		this.frenteRepo.save(frente1);
		this.frenteRepo.save(frente2);
	}
	
	private void transformFile(MultipartFile img,String uuid,String rol,String endpoint,String userToken,String seguridad) throws CPException
	{
		// FIRMA PNG
		final byte[] PNG_MAGIC  = {(byte)0x89, 0x50, 0x4E, 0x47};
		// FIRMA JPEG
		final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
		
		boolean isPng = false;
		boolean isJpg = true;
		
		// PRIMERO SE BUSCA SI EXISTE UN FICHERO CON UN UUID EXISTENTE Y SE BORRA
		
		File dir = new File(CPConstants.IMG_PATH);
		
		File file = null;
		
		for(File item:dir.listFiles())
		{
			
			if(item.getName().indexOf(uuid) != -1)
			{
				file = item;
				break;
			}
		}
		
		if(file!=null)
		{
			file.delete();
		}
		
		try
		{
			InputStream is = img.getInputStream();
			byte[] header = is.readNBytes(4);
			
			
			// Comprobación de las cabeceras del fichero, deben de coincidir con el magic number de un PNG si no coincide da false
			isPng = header.length >= 4 
					&& header[0] == PNG_MAGIC[0] && header[1] == PNG_MAGIC[1]
					&& header[2] == PNG_MAGIC[2] && header[3] == PNG_MAGIC[3];
			
			isJpg = header.length >= 3
		            && header[0] == JPEG_MAGIC[0] && header[1] == JPEG_MAGIC[1]
		            && header[2] == JPEG_MAGIC[2];
		}
		catch(IOException ex)
		{
			log.error("[ERROR] -- {} -- {} Ha saltado un error IOException al leer los bytes del fichero -- {}",endpoint,userToken,seguridad);
			isPng = false;
			isJpg = false;
		}
		catch(IndexOutOfBoundsException ex)
		{
			log.warn("[ERROR] -- {} -- {} Ha saltado un error IndexOutOfBoundsException debido a que el fichero no posee el número de bytes necesario (4) para la validación de un magic number -- {}",endpoint,userToken,seguridad);
			isPng = false;
			isJpg = false;
		}
		
		// Ternario para establecer si el archivo es png o jpg 
		String header = isPng ? ".png" : "";
		header = isJpg ? ".jpg" : header;
		
		String filename = uuid+header;
		
		Path storageRoot = Paths.get(CPConstants.IMG_PATH).toAbsolutePath().normalize();
		Path destination = storageRoot.resolve(filename).normalize();
		
		if(!destination.startsWith(storageRoot))
		{
			log.warn("[AVISO] -- {} -- {} Ha intentado realizar un path traversal al incluir una imagen con permiso de {} -- {}",endpoint,userToken,rol,seguridad);
			throw new CPException(404,"Datos invalidos");
		}
		InputStream is = null;
		try
		{
			is = img.getInputStream();
			// Escritura del fichero y sobreescritura en caso de que exista para evitar errores
			Files.copy(is, destination,StandardCopyOption.REPLACE_EXISTING);
		}
		catch(IOException ex)
		{
			log.error("[ERROR] -- {} -- {} Ha saltado un error IOException al escribit con InputStream la imagen subida mediante POST -- {}",endpoint,userToken,seguridad);
			log.error("[DETAILS] {}",ex.getMessage());
			throw new CPException(500,"Error de servidor");
		}
		finally
		{
			if(is!=null)
			{
				try
				{
					is.close();
				}
				catch(IOException ex)
				{
					log.error("[ERROR] -- {} -- {} Ha saltado un error IOException por cerrar erroneamente un InputStream -- {}",endpoint,userToken,seguridad);
					throw new CPException(500,"Error de servidor");
				}
			}
		}
	}
	
	private byte [] loadImg(String uuid,String endpoint,String rol,String seguridad,String userToken) throws CPException
	{
		Resource resource = null;
		
		byte [] imgB64 = null;
		
		Path storageRoot = Paths.get(CPConstants.IMG_PATH).toAbsolutePath().normalize();
		
		File dir = new File(CPConstants.IMG_PATH);
		
		File file = null;
		
		for(File item:dir.listFiles())
		{
			
			if(item.getName().indexOf(uuid) != -1)
			{
				file = item;
				break;
			}
		}
		
		try
		{
			if(file != null)
			{
				Path filePath = storageRoot.resolve(file.getName()).normalize();
				resource = new UrlResource(filePath.toUri());
				if(!resource.exists() || !resource.isReadable())
				{
					log.warn("[AVISO] -- {} -- {} Se ha cargado la imagen del producto {} erroneamente -- {}",endpoint,userToken,uuid,seguridad);
				}
			}
		}
		catch(MalformedURLException ex)
		{
			log.error("[ERROR] -- {} -- {} Ha saltado un error MalformedURLException al rescatar la imagen del producto {} -- {}",endpoint,userToken,uuid,seguridad);
			log.error("[DETAILS] {}",ex.getMessage());
		}
		
		try
		{
			imgB64 = resource.getContentAsByteArray();
		}
		catch(IOException ex)
		{
			log.error("[ERROR] -- {} -- {} Ha saltado un error IOException al transformar la imagen a un array de bytes {} -- {}",endpoint,userToken,uuid,seguridad);
			log.error("[DETAILS] {}",ex.getMessage());
		}
		
		return imgB64;
	}

}