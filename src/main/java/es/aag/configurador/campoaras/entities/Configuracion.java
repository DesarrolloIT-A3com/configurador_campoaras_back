package es.aag.configurador.campoaras.entities;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="configuracion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Configuracion {

    @Id
    @Column(nullable = false, unique = true)
    private String referencia;

    @Column(nullable = false)
    private float fondo;

    @Column(nullable = false)
    private float ancho;

    @Column(nullable = false)
    private float alto;
    
    @Column(nullable = false)
    private float altoMax;
    
    @Column(nullable = false)
    private float fondoMin;
    
    @Column(nullable = false)
    private float fondoMax;
    
    @Column(nullable = false)
    private float precioMedidaFondoEsp;
    
    @Column(nullable = false)
    private float precioMedidaAnchoEsp;
    
    @Column(nullable = false)
    private float precioMedidaAltoEsp;
    
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<Map<String,Object>> armazon;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = true, columnDefinition = "json")
    private List<Map<String,Object>> extras;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "serie_id")
    private Serie serie;
    
    @OneToMany(mappedBy = "configuracion")
	private Set<ProductoConfigurado> series = new HashSet<ProductoConfigurado>();
    
}

