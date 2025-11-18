package modelo.vo;

import javax.annotation.processing.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import modelo.vo.GestosPersonas;

@Generated(value="org.eclipse.persistence.internal.jpa.modelgen.CanonicalModelProcessor", date="2025-11-16T21:17:40", comments="EclipseLink-2.7.12.v20230209-rNA")
@StaticMetamodel(Gestos.class)
public class Gestos_ { 

    public static volatile SingularAttribute<Gestos, String> significado;
    public static volatile ListAttribute<Gestos, GestosPersonas> gestosPersonasList;
    public static volatile SingularAttribute<Gestos, byte[]> imagen;
    public static volatile SingularAttribute<Gestos, Integer> id;

}