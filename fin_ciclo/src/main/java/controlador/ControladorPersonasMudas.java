package controlador;

import controlador.factory.HibernateUtil;
import java.io.IOException;
import java.sql.Time;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;
import modelo.dao.GestoDAO;
import modelo.vo.Gestos;
import org.hibernate.Session;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class ControladorPersonasMudas extends Thread {

    private static Session session;
    private static GestoDAO gDAO;

    private final Label lblMensaje;
    private final ImageView imageView;

    private VideoCapture camara;
    private volatile boolean ejecutando = true;
    private final String textoMensaje;

    private Mat frameAnterior = null;
    private boolean gestodetectado = false;
    private int idSignoActual = -1;
    private int framesSinMovimiento = 0;

    static {
        System.load("C:\\opencv\\build\\java\\x64\\opencv_java451.dll");
    }

    public ControladorPersonasMudas(ImageView imageView, Label lblMensaje) {
        this.imageView = imageView;
        this.lblMensaje = lblMensaje;
        this.textoMensaje = lblMensaje.getText();
    }

    public static void iniciarSession() {
        session = HibernateUtil.getSessionFactory().openSession();
        gDAO = HibernateUtil.getGestoDAO();
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
            while (ejecutando) {
                Mat frame = new Mat();
                if (camara.read(frame)) {
                    procesarFrame(frame);
                    mostrarFrame(frame);
                }
                frame.release();
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            liberarRecursos();
        }
    }

    private void procesarFrame(Mat frame) {
        try {
            Mat gray = new Mat();
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.GaussianBlur(gray, gray, new Size(21, 21), 0);
            Imgproc.resize(gray, gray, new Size(640, 480));

            if (frameAnterior == null) {
                frameAnterior = gray.clone();
                gray.release();
                return;
            }

            Mat diff = new Mat();
            Core.absdiff(frameAnterior, gray, diff);
            Imgproc.threshold(diff, diff, 15, 255, Imgproc.THRESH_BINARY);
            double movimiento = Core.sumElems(diff).val[0];

            if (movimiento > 50000) {
                framesSinMovimiento = 0;

                if (!gestodetectado) {
                    gestodetectado = true;
                    Platform.runLater(() -> lblMensaje.setText("Analizando..."));

                    Mat reducido = new Mat();
                    Imgproc.resize(gray, reducido, new Size(100, 100));
                    int detectedId = detectarSigno(reducido);
                    reducido.release();

                    if (detectedId != -1) {
                        idSignoActual = detectedId;
                        Gestos gesto = gDAO.obtenerId(session, idSignoActual);
                        guardarGestoPersona(frame, gesto);
                        Platform.runLater(() -> lblMensaje.setText(gesto.getSignificado() + " \n"));
                    }
                }

            } else {
                framesSinMovimiento++;
                if (framesSinMovimiento > 10 && gestodetectado) {
                    gestodetectado = false;
                    idSignoActual = -1;
                    Platform.runLater(() -> lblMensaje.setId("Fallo en la detección del gesto.\nPulse el botón de reinicio."));
                }
            }

            frameAnterior.release();
            frameAnterior = gray.clone();
            diff.release();
            gray.release();
        } catch (Exception e) {
        }
    }

    private int detectarSigno(Mat frameReducido) {
        double mejorError = Double.MAX_VALUE;
        int mejorId = -1;

        try {
            HibernateUtil.beginTx(session);
            List<Gestos> listaGestos = gDAO.listarGestos(session);
            

            for (Gestos g : listaGestos) {
                byte[] data = g.getImagen();
                Mat matBD = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_GRAYSCALE);
                Imgproc.resize(matBD, matBD, new Size(100, 100));

                if (!frameReducido.size().equals(matBD.size())) {
                    matBD.release();
                    continue;
                }

                Mat diff = new Mat();
                Core.absdiff(frameReducido, matBD, diff);
                double error = Core.sumElems(diff).val[0];

                if (error < mejorError * 0.9) {
                    mejorError = error;
                    mejorId = g.getId();
                }

                diff.release();
                matBD.release();
                HibernateUtil.commitTx(session);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return (mejorError < 900000) ? mejorId : -1;
    }

    private void guardarGestoPersona(Mat frame, Gestos idGesto) {
        try {
            session.beginTransaction();
            Date fecha = Date.from(Instant.now());
            Time hora = new Time(System.currentTimeMillis());
            String nombreArchivo = "C:\\ProgramData\\MySQL\\MySQL Server 8.0\\Uploads\\frame_" + idGesto + "_" + System.currentTimeMillis() + ".jpg";
            Imgcodecs.imwrite(nombreArchivo, frame);
            byte[] imagen = frameToBytes(frame);
            gDAO.insertar(session, imagen, fecha, hora, idGesto);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            HibernateUtil.commitTx(session);
        }
    }

    private byte[] frameToBytes(Mat frame) throws IOException {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", frame, mob);
        return mob.toArray();
    }

    private void mostrarFrame(Mat frame) {
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);

        int w = rgb.cols(), h = rgb.rows();
        byte[] buffer = new byte[w * h * 3];
        rgb.get(0, 0, buffer);

        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        byte[] copy = buffer.clone();

        Platform.runLater(() -> {
            pw.setPixels(0, 0, w, h, PixelFormat.getByteRgbInstance(), copy, 0, w * 3);
            imageView.setImage(img);
        });

        rgb.release();
    }

    public void detener() {
        ejecutando = false;
        this.interrupt();

        try {
            this.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void liberarRecursos() {
        if (camara != null && camara.isOpened()) {
            camara.release();
        }
        if (frameAnterior != null) {
            frameAnterior.release();
            frameAnterior = null;
        }
    }

    public ControladorPersonasMudas reiniciar() {
        detener();
        Platform.runLater(() -> lblMensaje.setText(textoMensaje));
        ControladorPersonasMudas nuevo = new ControladorPersonasMudas(imageView, lblMensaje);
        nuevo.start();
        return nuevo;
    }
}
