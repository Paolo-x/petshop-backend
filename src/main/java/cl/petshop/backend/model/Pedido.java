
package cl.petshop.backend.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pedido")
public class Pedido {

    public enum Estado {
        PENDIENTE,
        PAGADA,
        FALLIDA
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String numero;

    // Usuario que realiza la compra
    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @Column(nullable = false, updatable = false)
    private Instant fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Estado estado = Estado.PENDIENTE;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    // Datos de entrega
    @Column(nullable = false, length = 50)
    private String nombreEntrega;

    @Column(nullable = false, length = 100)
    private String apellidosEntrega;

    @Column(nullable = false, length = 100)
    private String correoEntrega;

    @Column(nullable = false, length = 200)
    private String calle;

    @Column(length = 100)
    private String departamento;

    @Column(nullable = false, length = 100)
    private String region;

    @Column(nullable = false, length = 100)
    private String comuna;

    @Column(length = 500)
    private String indicaciones;

    public Pedido() {
    }

    @PrePersist
    public void asignarFecha() {
        if (fecha == null) {
            fecha = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public Usuario getCliente() {
        return cliente;
    }

    public void setCliente(Usuario cliente) {
        this.cliente = cliente;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void setFecha(Instant fecha) {
        this.fecha = fecha;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getNombreEntrega() {
        return nombreEntrega;
    }

    public void setNombreEntrega(String nombreEntrega) {
        this.nombreEntrega = nombreEntrega;
    }

    public String getApellidosEntrega() {
        return apellidosEntrega;
    }

    public void setApellidosEntrega(String apellidosEntrega) {
        this.apellidosEntrega = apellidosEntrega;
    }

    public String getCorreoEntrega() {
        return correoEntrega;
    }

    public void setCorreoEntrega(String correoEntrega) {
        this.correoEntrega = correoEntrega;
    }

    public String getCalle() {
        return calle;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getComuna() {
        return comuna;
    }

    public void setComuna(String comuna) {
        this.comuna = comuna;
    }

    public String getIndicaciones() {
        return indicaciones;
    }

    public void setIndicaciones(String indicaciones) {
        this.indicaciones = indicaciones;
    }
}
