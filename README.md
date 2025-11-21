# proyectoFinalDAM

## **CLONAR REPOSITORIO**

git clone https://github.com/XoanV/proyectoFinalDAM.git

## **DEPENDENCIAS** 
(Se añaden en el pom.xml)

- MySQLConector: Esta dependecia sirve para conectarse a la base de datos remotamente. Versión: 8.0.33.

- Hibernate: Esta dependecia sirve para mapear objetos a tablas y para persistir los datos. Versión: 5.6.0.

- Lombok: Esta dependecia sirve para evitar la repetición de código. (Suprime los get y los set de las clases) Versión: 1.18.30.

- JavaFX: Esta dependencia sirve para aplicar hojas de estilos a java. Versión: 21.0.9.

## **BIBLIOTECAS**

- OpenCV: Esta librería sirve para procesar los frames y activar la cámara.

- MachineLearning: Sirve para reconocer los gestos.

## **PROGRAMAS**

- NetBeans: Este programa se utiliza para crear el código y ejecutarlo en la versión de prueba. 

- MySQLWorkbench: Este programa se utiliza para crear y visualizar las tablas de nuestra base de datos.

## **INSTALAR OPENCV EN NETBEANS**

1. Descargamos OpenCV. (https://opencv.org/releases/)

2. Tenemos que modificar el pom añadiendo la dependencia de OpenCV.

3. Luego tenemos que ir a las Propiedades del proyecto y a la sección *Run*.

4. Por último, en VMOptions poner esto: **-Djava.library.path="C:\\OpenCV\\build\\java\\x64"**.

## **JAVAFX EN NETBEANS**

Para poder personalizar nuestra ventana a través de una hoja de estilos hay que:

1. Descargar javaFX. (https://gluonhq.com/products/javafx/)

2. Agregar las dependencias correctas en el pom.xml.

3. Luego tenemos que ir a las Propiedades del proyecto y a la sección *Run*.

4. Por último, en VMOptions poner esto: **--module-path "C:\javafx-sdk-21.0.9\lib" --add-modules javafx.controls,javafx.fxml javafx.swing**.

## **DESPLIEGUE DEL PROYECTO**

Los pasos para desplegar el proyecto son los siguientes:

1. Primero, con el botón derecho, hacemos clic en nuestro proyecto, y vamos a **Properties > Run > y elegimos nuestra clase main**. 

2. Luego, con el botón derecho, hacemos clic en nuestro proyecto y le damos a la opción **Clean and build**.

3.  Lo siguiente que hacemos, es localizar el **.jar** en nuestra carpeta del proyecto.

4.  Una vez localizado, nos vamos al Símbolo del sistema (cmd) y vamos a la ruta donde esta el jar.

5.  Por último, ejecutamos el comando **java -jar elnombredenuestroproyecto.jar**.
