/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package modelo.dao;

import java.util.List;
import modelo.vo.Gestos;
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
}
