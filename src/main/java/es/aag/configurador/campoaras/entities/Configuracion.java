package es.aag.configurador.campoaras.entities;

import java.util.List;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
@Table(name="configuracion")
@Data
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

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<Map<String, Float>> armazon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_acabado_id")
    private Color colorArmazon;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "frente_id")
    private Frente frente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "json")
    private List<Map<String, Float>> armazon_frente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_acabado_frente_id")
    private Color colorFrente;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Map<String, Float>> tirador;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Map<String, Float>> regleta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_acabado_tirador_id")
    private Color colorTirador;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "color_acabado_regleta_id")
    private Color colorRegleta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "serie_id")
    private Serie serie;
}

