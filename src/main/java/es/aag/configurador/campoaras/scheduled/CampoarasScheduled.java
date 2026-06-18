package es.aag.configurador.campoaras.scheduled;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import es.aag.configurador.campoaras.entities.BulkProductosUsuario;
import es.aag.configurador.campoaras.entities.Pedido;
import es.aag.configurador.campoaras.entities.PedidoBackup;
import es.aag.configurador.campoaras.entities.PedidoBackup;
import es.aag.configurador.campoaras.entities.ProductoConfigurado;
import es.aag.configurador.campoaras.entities.Usuario;
import es.aag.configurador.campoaras.repositories.IBulkProductosUsuarioRepository;
import es.aag.configurador.campoaras.repositories.IPedidoBackupRepository;
import es.aag.configurador.campoaras.repositories.IPedidoRepository;
import es.aag.configurador.campoaras.repositories.IProductoConfiguradoRepository;
import es.aag.configurador.campoaras.repositories.IUsuarioRepository;
import es.aag.configurador.campoaras.services.EncryptorService;

@Component
public class CampoarasScheduled 
{
	private final Logger log = LogManager.getLogger();
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	@Autowired
	private IBulkProductosUsuarioRepository bulkRepo;
	
	@Autowired
	private IProductoConfiguradoRepository productoRepo;
	
	@Autowired
	private IUsuarioRepository userRepo;
	
	@Autowired
	private IPedidoRepository pedidoRepo;
	
	@Autowired
	private IPedidoBackupRepository pedidoBakRepo;
	
	@Autowired
	private EncryptorService encryptor;
	
	
	@Scheduled(
		    cron = "0 0 2 * * *",
		    zone = "Europe/Madrid"
		)
	@Transactional
	public void deleteOldConfiguration()
	{
		LocalDateTime now = LocalDateTime.now().minusDays(45);
		log.info("[ADMIN] -- /schedule -- Comienzo del proceso de eliminación de configuraciones obsoletas");
		List<BulkProductosUsuario> bulks = this.bulkRepo.findAll();
		List<PedidoBackup> pedidosBacks = this.pedidoBakRepo.findAll();
		List<Pedido> pedidos = this.pedidoRepo.findAll();
		List<BulkProductosUsuario> toDelete = new LinkedList<BulkProductosUsuario>();
		List<ProductoConfigurado> selecciones = new LinkedList<ProductoConfigurado>();
		List<PedidoBackup> backDelete = new LinkedList<PedidoBackup>();
		List<Usuario> affected = new LinkedList<Usuario>();
		List<Usuario> affectedBack = new LinkedList<Usuario>();
		
		for(BulkProductosUsuario item:bulks)
		{
			int size = 0;
			LocalDateTime other = item.getFecha();
			
			boolean isOld = true;
			
			if(other!=null)
			{
				isOld = now.isAfter(other);
			}
			else
			{
				log.info("[ADMIN] -- /schedule -- Fecha de la configuracion {} nula se procederá a borrado",this.encryptor.decrypt(item.getReferencia()));
			}
			
			if(isOld)
			{
				String referencia = this.encryptor.decrypt(item.getReferencia());
				boolean isInPedido = false;
				Usuario usuario = item.getUsuarioUuid();
				
				for(Pedido pedido:pedidos)
				{
					String refPedido = this.encryptor.decrypt(pedido.getReferencia());
					
					if(pedido.getUsuarioPedido().equals(usuario) && refPedido.equals(referencia))
					{
						isInPedido = true;
						break;
					}
				}
				
				if(!isInPedido)
				{
					usuario.removeBulk(item);
					
					
					// Buscamos las selecciones dentro de la cesta para añadirlas a la lista de borrado
					for(String seleccion:item.getProductos())
					{
						Optional<ProductoConfigurado> optSel = this.productoRepo.findById(seleccion);
						
						if(optSel.isPresent())
						{
							selecciones.add(optSel.get());
							size++;
						}
					}
					
					toDelete.add(item);
					affected.add(usuario);
					log.info("[ADMIN] -- /scheduled -- Se han detectado {} configuraciones obsoletas del usuario {}",size,usuario.getUSRToken());
				}
			}
		}
		
		for(PedidoBackup pedido:pedidosBacks)
		{
			boolean isOld = true;
			LocalDateTime other = pedido.getFecha();
			
			if(other!=null)
			{
				isOld = now.isAfter(other);
			}
			else
			{
				log.info("[ADMIN] -- /schedule -- Fecha de pedido back {} nula, se procederá a su borrado",this.encryptor.decrypt(pedido.getReferencia()));
			}
			
			if(isOld)
			{
				Usuario usuario = pedido.getUsuarioPedidoBack();
				usuario.removePedidoBak(pedido);
				affectedBack.add(usuario);
				backDelete.add(pedido);
				log.info("[ADMIN] -- /scheduled -- Se han detectado {} copias de pedido obsoletas del usuario {}",backDelete.size(),usuario.getUSRToken());
			}
			
		}
		
		if(affected.isEmpty() || toDelete.isEmpty() || selecciones.isEmpty())
		{
			log.info("[ADMIN] -- /schedule -- No se han encontrado configuraciones a borrar");
		}
		else
		{
			log.info("[ADMIN] -- /schedule -- Se han borrado {} configuraciones obsoletas",selecciones.size());
			this.userRepo.saveAll(affected);
			this.userRepo.flush();
			this.productoRepo.deleteAll(selecciones);
			this.productoRepo.flush();
			this.bulkRepo.deleteAll(toDelete);
			this.bulkRepo.flush();
			
		}
		
		if(backDelete.isEmpty())
		{
			log.info("[ADMIN] -- /schedule -- No se han encontrado copias de pedido a borrar");
		}
		else
		{
			log.info("[ADMIN] -- /schedule -- Se han borrado {} copias de pedido obsoletos",backDelete.size());
			this.userRepo.saveAll(affectedBack);
			this.userRepo.flush();
			this.pedidoBakRepo.deleteAll(backDelete);
			this.pedidoBakRepo.flush();
		}
	}
	
//	@Scheduled(fixedRate = 5 * 60 * 1000) // 4 minutos y medio
//	@Transactional
//	public void deleteOldOrders()
//	{
//		LocalDateTime now = LocalDateTime.now().minusDays(45);
//		log.info("[ADMIN] -- /schedule -- Comienzo del proceso de eliminación de pedidos obsoletos");
//		List<Pedido> pedidos = this.pedidoRepo.findAll();
//		List<Pedido> toDelete = new LinkedList<Pedido>();
//		List<Usuario> affected = new LinkedList<Usuario>();
// 		
//		for(Pedido item:pedidos)
//		{
//			LocalDateTime other = item.getFecha();
//			
//			boolean isOld = true;
//			
//			if(other!=null)
//			{
//				isOld = now.isAfter(other);
//			}
//			else
//			{
//				log.info("[AVISO] Fecha en pedidos nula");
//			}
//			
//			if(isOld)
//			{
//				Usuario usuario = item.getUsuarioPedido();
//				usuario.removePedidos(item);
//				
//				toDelete.add(item);
//				affected.add(usuario);
//			}
//		}
//		
//		if(affected.isEmpty() || toDelete.isEmpty())
//		{
//			log.info("[ADMIN] -- /schedule -- No se han encontrado pedidos a borrar");
//		}
//		else
//		{
//			log.info("[ADMIN] -- /schedule -- Se han borrado {} pedidos obsoletos",toDelete.size());
//			this.pedidoRepo.deleteAll(toDelete);
//			this.pedidoRepo.flush();
//			this.userRepo.saveAll(affected);
//			this.userRepo.flush();
//		}
//		
//
//	}
}
