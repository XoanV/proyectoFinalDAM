package controlador;

import controlador.factory.HibernateUtil;
import modelo.dao.GestoDAO;
import modelo.vo.Gestos;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import org.hibernate.Session;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;

import java.sql.Time;
import java.util.Date;

public class ControladorPersonasMudas extends Thread {

    private static Session session;
    private static GestoDAO gDAO;

    private final Label lblMensaje;
    private final ImageView imageView;

    private VideoCapture camara;
    private volatile boolean ejecutando = true;

    private Mat frameAnterior = null;
    private boolean gestoDetectado = false;
    private final String textoOriginal;

    private final modeloGestos modeloML;

    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java451.dll");
    }

    public ControladorPersonasMudas(ImageView imageView, Label lblMensaje) {
        this.imageView = imageView;
        this.lblMensaje = lblMensaje;
        this.textoOriginal = lblMensaje.getText();
        this.modeloML = new modeloGestos("C:/Users/veiga/Desktop/Proyecto/proyectoFinalDAM/fin_ciclo/modelo/saved_model");
    }

    public static void iniciarSession() {
        session = HibernateUtil.getSessionFactory().openSession();
        gDAO = new GestoDAO();
    }

    public static void cerrarSession() {
        if (session != null && session.isOpen()) session.close();
    }

    @Override
    public void run() {
        ejecutando = true;
        camara = new VideoCapture(0);
        if (!camara.isOpened()) {
            Platform.runLater(() -> lblMensaje.setText("No se pudo abrir la cámara."));
            return;
        }

        try {
            while (ejecutando && camara.isOpened()) {
                Mat frame = new Mat();
                try {
                    if (camara.read(frame)) {
                        procesarFrame(frame);
                        mostrarFrame(frame);
                    }
                } finally {
                    frame.release();
                }
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            liberarRecursos();
        }
    }

    private void procesarFrame(Mat frame) {
    Mat gray = new Mat();
    Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
    Imgproc.GaussianBlur(gray, gray, new Size(21, 21), 0);

    if (frameAnterior == null) {
        frameAnterior = gray.clone();
        gray.release();
        return;
    }

    Mat diff = new Mat();
    Core.absdiff(frameAnterior, gray, diff);
    Imgproc.threshold(diff, diff, 20, 255, Imgproc.THRESH_BINARY);

    double movimiento = Core.sumElems(diff).val[0];

    if (movimiento > 5000) {
        if (!gestoDetectado) {
            gestoDetectado = true;
            Platform.runLater(() -> lblMensaje.setText("Analizando..."));

            // --- NUEVO BLOQUE: convertir frame a 4D para MobileNetV2 ---
            Mat resized = new Mat();
            Imgproc.resize(frame, resized, new Size(224, 224));
            Imgproc.cvtColor(resized, resized, Imgproc.COLOR_BGR2RGB);

            float[][][][] input = new float[1][224][224][3];
            for (int y = 0; y < 224; y++) {
                for (int x = 0; x < 224; x++) {
                    double[] pixel = resized.get(y, x);
                    input[0][y][x][0] = (float) pixel[0] / 255.0f;
                    input[0][y][x][1] = (float) pixel[1] / 255.0f;
                    input[0][y][x][2] = (float) pixel[2] / 255.0f;
                }
            }
            resized.release();

            int predIndex = modeloML.predecir(input);

            if (predIndex != -1) {
                String labelPredicho = modeloML.getLabel(predIndex);
                int idGestoBD = gDAO.obtenerIdPorSignificado(session, labelPredicho);

                if (idGestoBD != -1) {
                    Gestos g = gDAO.obtenerId(session, idGestoBD);
                    if (g != null) {
                        guardarGestoPersona(frame, g);
                        Platform.runLater(() -> lblMensaje.setText(" " + g.getSignificado()));
                    }
                } else {
                    gestoDetectado = false;
                    Platform.runLater(() -> lblMensaje.setText("Gesto no reconocido, pulse reiniciar."));
                }
            } else {
                gestoDetectado = false;
            }
        }
    }

    frameAnterior.release();
    frameAnterior = gray.clone();
    diff.release();
    gray.release();
}


    private void guardarGestoPersona(Mat frame, Gestos g) {
        try {
            Date fecha = new Date();
            Time hora = new Time(System.currentTimeMillis());

            session.beginTransaction();
            byte[] imagen = convertirFrameABytes(frame);
            gDAO.insertar(session, imagen, fecha, hora, g);
            session.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (session.getTransaction().isActive()) session.getTransaction().rollback();
        }
    }

    private byte[] convertirFrameABytes(Mat frame) {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, mob);
        return mob.toArray();
    }

    private void mostrarFrame(Mat frame) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);

        int w = rgb.cols();
        int h = rgb.rows();
        byte[] buffer = new byte[w * h * 3];
        rgb.get(0, 0, buffer);

        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        pw.setPixels(0, 0, w, h, PixelFormat.getByteRgbInstance(), buffer, 0, w * 3);

        Platform.runLater(() -> imageView.setImage(img));
        rgb.release();
    }

    public void detener() {
        ejecutando = false;
        this.interrupt();
        try {
            this.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            liberarRecursos();
        }
    }

    public ControladorPersonasMudas reiniciar() {
        detener();
        Platform.runLater(() -> lblMensaje.setText(textoOriginal));
        try {
            HibernateUtil.beginTx(session);
            gDAO.borrar(session);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
        }
        ControladorPersonasMudas hiloNuevo = new ControladorPersonasMudas(imageView, lblMensaje);
        hiloNuevo.start();
        return hiloNuevo;
    }

    private void liberarRecursos() {
        if (camara != null && camara.isOpened()) camara.release();
        if (frameAnterior != null) frameAnterior.release();
    }
}

