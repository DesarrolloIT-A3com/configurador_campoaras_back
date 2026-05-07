package es.aag.configurador.campoaras.services;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.aag.configurador.campoaras.dto.OrderDTO;
import es.aag.configurador.campoaras.dto.ResponseSeleccion;
import es.aag.configurador.campoaras.dto.SeleccionDTO;
import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.Pedido;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IBulkProductosUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IPedidoRepository;
import es.aag.configurador.campoaras.repositories.IProductoConfiguradoRepository;
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
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IProductoConfiguradoRepository seleccionRepo;
	
	@Autowired
	private IBulkProductosUsuarioRepository bulkRepo;
	
	@Autowired
	private IPedidoRepository pedidoRepo;
	
	
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
				if(bulk.getProductos().isEmpty())
				{
					this.bulkRepo.delete(bulk);
				}
				else
				{
					this.bulkRepo.save(bulk);
				}
				this.bulkRepo.flush();
				break;
			}
		}
		
		
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
		
		for(String producto:body.getProductos())
		{
			Optional<ProductoConfigurado> optSeleccion = this.seleccionRepo.findById(producto);
			
			if(!optSeleccion.isPresent())
			{
				log.warn("[AVISO] -- /order-proposal -- {} Ha intentado tramitar un pedido introduciendo un uuid de seleccion erroneo con permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(404,"Datos inexistentes");
			}
					
			if(!usuario.getUuid().equals(body.getUsuario()))
			{
				log.warn("[AVISO] -- /order-proposal -- {} Ha intentado tramitar un pedido usando un uuid de usuario diferente al suyo con permiso de {} -- {}",usrToken,rol,seguridad);
				throw new CPException(400,"Datos inválidos");
			}
			
			productoList.add(producto);
		}
		
		String uuid = UUID.randomUUID().toString();
		
		pedido.setUuid(uuid);
		pedido.setProductos(productoList);
		pedido.setReferencia(this.encryptor.encrypt(body.getReferencia()));
		pedido.setUsuarioPedido(usuario);
		pedido.setEstado(EstadoPedido.NO_ENVIADO);
		
		log.info("[ACCION] -- /order-proposal -- {} Ha tramitado un pedido con permiso de {} -- {}",usrToken,rol,seguridad);
		
		this.pedidoRepo.save(pedido);
		this.pedidoRepo.flush();
		usuario.addPedidos(pedido);
		this.userRepo.save(usuario);
		this.userRepo.flush();
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
					String referenciaSel = this.encryptor.decrypt(seleccion.getFrente().getReferencia())+" "+seleccion.getConfiguracion().getReferencia();					
					String armazon = this.encryptor.decrypt(seleccion.getAcabado().getNombre());
					String colorArmazon = this.encryptor.decrypt(seleccion.getColorArmazon().getNombre());
					String frente = this.encryptor.decrypt(seleccion.getFrente().getNombre());
					String acabadoFrente = this.encryptor.decrypt(seleccion.getAcabadoFrente().getNombre());
					String colorFrente = this.encryptor.decrypt(seleccion.getColorFrente().getNombre());
					String acabadoRegleta = null;
					String acabadoTirador = null;
					String colorRegleta = null;
					String colorTirador = null;
					
					if(seleccion.getAcabadoRegleta()!=null)
					{
						acabadoRegleta = this.encryptor.decrypt(seleccion.getAcabadoRegleta().getNombre());
						colorRegleta = this.encryptor.decrypt(seleccion.getColorRegleta().getNombre());
					}
					
					if(seleccion.getAcabadoTirador()!=null)
					{
						acabadoTirador = this.encryptor.decrypt(seleccion.getAcabadoTirador().getNombre());
						colorTirador = this.encryptor.decrypt(seleccion.getColorTirador().getNombre());
					}
					
					// En caso de que no se hayan seleccionado medidas especiales se cogen las medidas base
					float fondo = seleccion.getFondo()!=null ? seleccion.getFondo() : seleccion.getConfiguracion().getFondo();
					float ancho = seleccion.getAncho()!=null ? seleccion.getAncho() : seleccion.getConfiguracion().getAncho();
					float alto = seleccion.getAlto()!=null ? seleccion.getAlto() : seleccion.getConfiguracion().getAlto();
					
					// Conversión a milimetros
					fondo = fondo * 10;
					ancho = ancho * 10;
					alto = alto * 10;
					
					float precioFinal = seleccion.getPrecioFinal();
					int cantidad = seleccion.getCantidad();
					
					String serie = this.encryptor.decrypt(seleccion.getConfiguracion().getSerie().getProducto().getNombre()) +" "+this.encryptor.decrypt(seleccion.getConfiguracion().getSerie().getVariante());					
					SeleccionDTO dto = new SeleccionDTO(uuidSel,referenciaSel,this.encryptor.decrypt(usuario.getUsername()),serie,fondo,ancho,alto,null,armazon,colorArmazon,null,frente,acabadoFrente,
							colorFrente,null,acabadoTirador,colorTirador,null,acabadoRegleta,colorRegleta,precioFinal,cantidad,null);
					
					selecciones.add(dto);
				}
			}
			response.add(new OrderDTO(uuid,referencia,this.encryptor.decrypt(usuario.getUsername()),null,estado, selecciones));
//			response.add(new OrderDTO(uuid,referencia,this.encryptor.decrypt(usuario.getUsername()),pedido.getFecha(),null,estado, selecciones));
			
		}
		
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
		
		this.userRepo.save(userPedido);
		this.userRepo.flush();;
	}
	
	public void actualizarEstado(EstadoPedido estado,String uuid,String rol,String seguridad,String usrToken) throws CPException
	{
		if(!rol.equals(CPConstants.ADMIN_ROLE) && rol.equals(CPConstants.SUPADMIN_ROLE))
		{
			if(!estado.equals(EstadoPedido.ENVIADO))
			{
				log.warn("[AVISO] -- /orders -- {} Ha intentado actualizar el estado de un pedido al valor {} el cual no está permitido con permiso de {} -- {}",usrToken,estado,rol,seguridad);
				throw new CPException(400,"Datos invalidos");
			}
		}
		else
		{
			EstadoPedido [] estados = {EstadoPedido.NO_ENVIADO,EstadoPedido.ENVIADO,EstadoPedido.ACEPTADO,EstadoPedido.RECHAZADO};
			
			if(Arrays.binarySearch(estados, estado)==-1)
			{
				log.warn("[AVISO] -- /orders -- {} Ha intentado actualizar el estado de un pedido al valor {} el cual no es válido con permiso de {} -- {}",usrToken,estado,rol,seguridad);
				throw new CPException(400,"Datos invalidos");
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
		
		log.info("[ACCION] -- /orders -- {} Ha actualizado el estado del pedido {} a {} con permiso de {} -- {}",usrToken,pedido.getUuid(),estado,rol,seguridad);
		
		this.pedidoRepo.save(pedido);
		this.pedidoRepo.flush();
		
		Usuario userPedido = pedido.getUsuarioPedido();
		userPedido.removePedidos(pedido);
		userPedido.addPedidos(pedido);
		
		this.userRepo.save(userPedido);
		this.userRepo.flush();
	}
	
}
