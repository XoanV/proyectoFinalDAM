package modelo.vo;

import java.util.Date;
import javax.annotation.processing.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import modelo.vo.Gestos;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2025-11-16T21:17:40", comments="EclipseLink-2.7.12.v20230209-rNA")
@StaticMetamodel(GestosPersonas.class)
public class GestosPersonas_ { 

    public static volatile SingularAttribute<GestosPersonas, Date> fecha;
    public static volatile SingularAttribute<GestosPersonas, Gestos> iDGesto;
    public static volatile SingularAttribute<GestosPersonas, Date> hora;
    public static volatile SingularAttribute<GestosPersonas, byte[]> imagenPersona;
    public static volatile SingularAttribute<GestosPersonas, Integer> id;

}