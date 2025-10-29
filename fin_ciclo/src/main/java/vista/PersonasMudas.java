
package vista;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import controlador.ControladorPersonasMudas;

public class PersonasMudas extends Application {

    @Override
    public void start(Stage stage) {
        Label lblMensaje = new Label("Comience a hacer gestos delante de la cámara\n y cuando termine sólo espere");
        Button btnReinicio = new Button("Reiniciar");

        ImageView imageView = new ImageView();
        imageView.setFitWidth(320);
        imageView.setFitHeight(240);
        imageView.setPreserveRatio(true);

        VBox root = new VBox(20, imageView, lblMensaje, btnReinicio);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 700, 400);
        stage.setTitle("Personas Mudas");
        stage.setScene(scene);
        stage.show();

        // Iniciar cámara
        ControladorPersonasMudas camara = new ControladorPersonasMudas(imageView);
        camara.start();
        
        // Detener hilo cuando se cierre la ventana
stage.setOnCloseRequest(e -> {
    camara.detener();  // Detiene el hilo de la cámara
});

        btnReinicio.setOnAction(e -> camara.reiniciar());
    }
}