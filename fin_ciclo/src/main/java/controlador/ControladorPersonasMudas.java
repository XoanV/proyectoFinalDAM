package controlador;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;

public class ControladorPersonasMudas extends Thread {

    private final ImageView imageView;
    private final VideoCapture camara;
    private final Mat frame;
    private volatile boolean ejecutando = true;

    static {
        // Ajusta la ruta según tu instalación de OpenCV
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java451.dll");
    }

    public ControladorPersonasMudas(ImageView imageView) {
        this.imageView = imageView;
        this.camara = new VideoCapture(0);
        this.frame = new Mat();
    }

    @Override
    public void run() {
        if (!camara.isOpened()) {
            System.out.println("No se pudo abrir la cámara");
            return;
        }

        while (ejecutando) {
            if (camara.read(frame)) {

                // Convertir BGR a RGB
                Mat rgbFrame = new Mat();
                Imgproc.cvtColor(frame, rgbFrame, Imgproc.COLOR_BGR2RGB);

                int width = rgbFrame.cols();
                int height = rgbFrame.rows();

                byte[] buffer = new byte[width * height * 3];
                rgbFrame.get(0, 0, buffer);

                WritableImage writableImage = new WritableImage(width, height);
                PixelWriter pixelWriter = writableImage.getPixelWriter();

                final byte[] bufferCopy = buffer.clone();

                Platform.runLater(() -> {
                    pixelWriter.setPixels(
                        0, 0, width, height,
                        PixelFormat.getByteRgbInstance(),
                        bufferCopy, 0, width * 3
                    );
                    imageView.setImage(writableImage);
                });
            }

            try {
                Thread.sleep(33); // ~30 FPS
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        camara.release();
    }

    public void detener() {
        ejecutando = false;
    }

    public void reiniciar() {
        System.out.println("Reiniciando cámara...");
        // Puedes agregar aquí lógica para reiniciar procesos
    }
}
