/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.dao;

import java.sql.Time;
import java.util.Date;
import java.util.List;
import modelo.vo.Gestos;
import modelo.vo.GestosPersonas;
import org.hibernate.Session;
import org.hibernate.query.Query;

/**
 *
 * @author Xoan Veiga
 */
public class GestoDAO {

    public void borrar(Session session) {
        session.createNativeQuery("DELETE FROM GestosPersonas").executeUpdate();
    }

    public List<Gestos> listarGestos(Session session) {
        Query q = session.createQuery("from Gestos g");
        List<Gestos> img = q.list();

        return img;
    }

    public void insertar(Session session, byte[] gesto, Date fecha, Time hora, Gestos idGestoImagen) {
        GestosPersonas gp = new GestosPersonas(gesto, fecha, hora, idGestoImagen);
        session.save(gp);
    }

    public Gestos obtenerId(Session session, int idActual) {
        return session.get(Gestos.class, idActual);
    }

    public int obtenerIdPorSignificado(Session session, String nom) {
        Gestos g; 
         Query q = session.createQuery("FROM Gestos g WHERE g.significado =:sig");
         q.setParameter("sig", nom);
         
         g = (Gestos) q.uniqueResult();
        if (g != null) {
            return g.getId();
        } else {
            return -1;
        }
    }
}
