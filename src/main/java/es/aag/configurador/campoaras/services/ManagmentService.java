package es.aag.configurador.campoaras.services;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
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
				
				// TODO: Debes de realizar sobre este bloque hasta el siguiente comentario una validación en la numeración, si en la numeración aparece un número repetido
				// ej: [1,2,3,4,5] y se añade un 4 la lista debe de pasar a ser [1,2,3,4,5,6] siendo que el 4 se mete en medio para ello aumentas el número de los productos
				// que sigan despues del 4
				List<Producto> productos = this.productoRepo.findAll();				
				productos.sort(Comparator.comparingInt(Producto::getOrden));

				int nuevoOrden = body.getOrden();
				boolean ordenRepetido = false;

				// Verificar si el orden ya existe con un for normal
				for(int i = 0; i < productos.size(); i++) {
				    if(productos.get(i).getOrden() == nuevoOrden) {
				        ordenRepetido = true;
				        break;
				    }
				}

				// Si el orden está repetido, desplazar los productos con orden >= nuevoOrden
				if(ordenRepetido) {
				    for(int i = 0; i < productos.size(); i++) {
				        Producto p = productos.get(i);
				        if(p.getOrden() >= nuevoOrden) {
				            p.setOrden(p.getOrden() + 1);
				            this.productoRepo.save(p);
				        }
				    }
				    this.productoRepo.flush();
				}

				// Asignar el orden al nuevo producto
				producto.setOrden(nuevoOrden);

				// Guardar el nuevo producto
				this.productoRepo.save(producto);
				this.productoRepo.flush();
				
				//--------------------------
				// FIN DE CAMBIOS DE LIBERTO | Descomenta toda esta descripción cuando termines
				//--------------------------
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
					int orden = item.getOrden();
					byte [] imgB64 = this.loadImg(uuidProducto, "/products", rol, seguridad, usrToken);
					response.add(new ResponseProducto(uuidProducto,nombre,tipo,cajon,orden,imgB64));
				}
				
				log.info("[ACCION] -- /products -- {} Ha solicitado un listado de productos con permiso de {} -- {}",usrToken,rol,seguridad);		
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
				
				// TODO: Debes de realizar sobre este bloque hasta el siguiente comentario una validación en la numeración, si en la numeración aparece un número repetido
				// ej: [1,2,3,4,5] y se añade un 4 la lista debe de pasar a ser [1,2,3,4,5,6] siendo que el 4 se mete en medio para ello aumentas el número de los productos
				// que sigan despues del 4
				List<Producto> productos = this.productoRepo.findAll();				
				productos.sort(Comparator.comparingInt(Producto::getOrden));

				int nuevoOrden = body.getOrden();
				boolean ordenRepetido = false;

				// Verificar si el orden ya existe con un for normal
				for(int i = 0; i < productos.size(); i++) {
				    if(productos.get(i).getOrden() == nuevoOrden) {
				        ordenRepetido = true;
				        break;
				    }
				}

				// Si el orden está repetido, desplazar los productos con orden >= nuevoOrden
				if(ordenRepetido) {
				    for(int i = 0; i < productos.size(); i++) {
				        Producto p = productos.get(i);
				        if(p.getOrden() >= nuevoOrden) {
				            p.setOrden(p.getOrden() + 1);
				            this.productoRepo.save(p);
				        }
				    }
				    this.productoRepo.flush();
				}

				// Asignar el orden al nuevo producto
				producto.setOrden(nuevoOrden);

				// Guardar el nuevo producto
				this.productoRepo.save(producto);
				this.productoRepo.flush();
				
				
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
				
				log.info("[ACCION] -- /series -- {} Ha solicitado un listado de series con permiso de {} -- {}",usrToken,rol,seguridad);
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
				
				log.info("[ACCION] -- /acabados -- {} Ha solicitado un listado de acabados con permiso de {} -- {}",usrToken,rol,seguridad);		
				
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
				
				log.info("[ACCION] -- /colores -- {} Ha solicitado un listado de colores con permiso de {} -- {}",usrToken,rol,seguridad);		
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
				
				log.info("[ACCION] -- /frentes -- {} Ha solicitado un listado de frentes con permiso de {} -- {}",usrToken,rol,seguridad);		
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
			if(resource!=null)
			{
				imgB64 = resource.getContentAsByteArray();
			}
		}
		catch(IOException ex)
		{
			log.error("[ERROR] -- {} -- {} Ha saltado un error IOException al transformar la imagen a un array de bytes {} -- {}",endpoint,userToken,uuid,seguridad);
			log.error("[DETAILS] {}",ex.getMessage());
		}
		
		return imgB64;
	}

}