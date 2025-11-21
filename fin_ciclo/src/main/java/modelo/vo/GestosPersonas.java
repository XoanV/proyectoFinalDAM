/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.vo;

import java.io.Serializable;
import java.sql.Time;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Xoan Veiga
 */
@Entity
@Table(name = "gestospersonas")

public class GestosPersonas implements Serializable {

    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID")
    private Integer id;
    @Basic(optional = false)
    @Lob
    @Column(name = "ImagenPersona")
    private byte[] imagenPersona;
    @Basic(optional = false)
    @Column(name = "Fecha")
    @Temporal(javax.persistence.TemporalType.DATE)
    private Date fecha;
    @Basic(optional = false)
    @Column(name = "Hora")
    private Time hora;
    @JoinColumn(name = "ID_Gesto", referencedColumnName = "ID")
    @ManyToOne(optional = false)
    private Gestos iDGesto;

    public GestosPersonas() {
    }

    public GestosPersonas(byte[] imagenPersona, Date fecha, Time hora, Gestos iDGesto) {
        this.imagenPersona = imagenPersona;
        this.fecha = fecha;
        this.hora = hora;
        this.iDGesto = iDGesto;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public byte[] getImagenPersona() {
        return imagenPersona;
    }

    public void setImagenPersona(byte[] imagenPersona) {
        this.imagenPersona = imagenPersona;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Time getHora() {
        return hora;
    }

    public void setHora(Time hora) {
        this.hora = hora;
    }

    public Gestos getIDGesto() {
        return iDGesto;
    }

    public void setIDGesto(Gestos iDGesto) {
        this.iDGesto = iDGesto;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof GestosPersonas)) {
            return false;
        }
        GestosPersonas other = (GestosPersonas) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }
}