package controlador;

import controlador.factory.HibernateUtil;
import java.sql.Time;
import java.util.Date;

import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;
import javax.swing.JOptionPane;

import modelo.dao.GestoDAO;
import modelo.vo.Gestos;

import org.hibernate.Session;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;

import org.opencv.videoio.VideoCapture;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;

public class ControladorPersonasMudas extends Thread {

    private static Session session;
    private static GestoDAO gDAO;

    private final Label lblMensaje;
    private final ImageView imageView;

    private VideoCapture camara;
    private volatile boolean ejecutando = true;

    private Mat frameAnterior = null;
    private boolean gestoDetectado = false;
    private int framesSinMovimiento = 0;

    private final modeloGestos modeloML;
    private final String textoOriginal;

    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java451.dll");
    }

    public ControladorPersonasMudas(ImageView imageView, Label lblMensaje) {
        this.imageView = imageView;
        this.lblMensaje = lblMensaje;
        this.textoOriginal = lblMensaje.getText();
        this.modeloML = new modeloGestos("C:/Users/veiga/Desktop/Proyecto/proyectoFinalDAM/fin_ciclo/modelo/modelo/");
    }

    public static void iniciarSession() {
        session = HibernateUtil.getSessionFactory().openSession();
        gDAO = new GestoDAO();
    }

    public static void cerrarSession() {
        if (session != null && session.isOpen()) {
            session.close();
        }
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
            framesSinMovimiento = 0;
            if (!gestoDetectado) {
                gestoDetectado = true;
                Platform.runLater(() -> lblMensaje.setText("Analizando..."));

                float[] keypoints = modeloML.extraerKeypoints(frame);

                int predIndex = modeloML.predecir(keypoints);
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
                            Platform.runLater(() -> lblMensaje.setText("Gesto incorrecto, por favor pulse el botón de reiniciar."));
                    }
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
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
        } finally {
            HibernateUtil.commitTx(session);
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
            JOptionPane.showMessageDialog(null, "Error al borrar");
        } finally {
            HibernateUtil.commitTx(session);
        }

        ControladorPersonasMudas hiloNuevo = new ControladorPersonasMudas(imageView, lblMensaje);
        hiloNuevo.start();
        return hiloNuevo;
    }

    private void liberarRecursos() {
        if (camara != null && camara.isOpened()) {
            camara.release();
        }
        if (frameAnterior != null) {
            frameAnterior.release();
        }
    }
}