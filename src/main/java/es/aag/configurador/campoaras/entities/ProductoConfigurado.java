package es.aag.configurador.campoaras.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "producto_configurado")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductoConfigurado 
{
	@Id
	@Column(nullable = false, unique = true)
	private String uuid;
	
	@Column(nullable = false)
	private float precioArmazon;
	
	@Column(nullable = false)
	private String tipoAcabadoArmazon;
	
	@Column(nullable = false)
	private float precioFrente;
	
	@Column(nullable = false)
	private String tipoAcabadoFrente;

	@Column(nullable = false)
	private float precioTirador;
	
	@Column(nullable = false)
	private String tipoAcabadoTirador;
	
	@Column(nullable = false)
	private float precioRegleta;
	
	@Column(nullable = false)
	private String tipoAcabadoRegleta;
	
	@Column(nullable = false)
	private int cantidad;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acabado_id")
    private Acabado acabado;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_acabado_id")
    private Color colorAcabado;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "frente_id")
    private Frente frente;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acabado_frente_id")
    private Acabado acabadoFrente;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_frente_id")
    private Color colorFrente;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acabado_regleta_id")
    private Acabado acabadoRegleta;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_regleta_id")
    private Color colorRegleta;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "acabado_tirador_id")
    private Acabado acabadoTirador;
	
	@ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_tirador_id")
    private Color colorTirador;
	
	
}
