/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controlador;

import org.opencv.core.Core;
import org.opencv.videoio.VideoCapture;
import vista.PersonasMudas;

/**
 *
 * @author Xoan Veiga
 */
public class ControladorPersonasMudas implements Runnable {
    

    public static PersonasMudas ventana = new PersonasMudas();
    
    public static void iniciar() {
        ventana.setVisible(true);
        ventana.setLocationRelativeTo(null);
    }
    
    @Override
    public void run() {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        VideoCapture camara = new VideoCapture(0);
    }
}
