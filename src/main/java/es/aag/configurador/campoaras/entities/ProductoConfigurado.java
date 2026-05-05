package es.aag.configurador.campoaras.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto_configurado")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductoConfigurado 
{
	@Id
	@Column(nullable = false, unique = true)
	private String uuid;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "referencia",nullable = false)
	private Configuracion configuracion;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "usuario_id",nullable = false)
	private Usuario usuario;
	
	@Column
	private float precioArmazon;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acabado_id")
    private Acabado acabado;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_armazon_id")
    private Color colorArmazon;
	
	@Column
	private float precioFrente;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "frente_id")
    private Frente frente;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acabado_frente_id")
    private Acabado acabadoFrente;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_frente_id")
    private Color colorFrente;

	@Column
	private float precioTirador;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acabado_tirador_id")
    private Acabado acabadoTirador;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_tirador_id")
    private Color colorTirador;
	
	@Column
	private float precioRegleta;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acabado_regleta_id")
    private Acabado acabadoRegleta;
	
	@ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "color_regleta_id")
    private Color colorRegleta;
	
	// Estas columnas son únicas para las medidas especiales u otras medidas las cuales oscilan un rango entre las medidas de una referencia
	@Column
	private Float fondo;
	
	@Column
	private Float ancho;
	
	@Column
	private Float alto;
	
	@Column
	private float precioFinal;
	
	@Column
	private int cantidad;
	
	@Column(nullable = false)
	private LocalDateTime fecha;
}
