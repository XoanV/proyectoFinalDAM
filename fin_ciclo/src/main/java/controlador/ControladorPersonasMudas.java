package controlador;

import controlador.factory.HibernateUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<String, Mat> signos = new HashMap<>();

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
        if (session != null && session.isOpen())
            session.close();
    }

    public void cargarSignosDesdeBD() {
        signos.clear();
        try {
            session.beginTransaction();
            List<Gestos> lista = gDAO.listarGestos(session);

            for (Gestos g : lista) {
                byte[] data = g.getImagen();
                String significado = g.getSignificado();
                Mat mat = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_GRAYSCALE);
                Imgproc.resize(mat, mat, new Size(100, 100));
                signos.put(significado, mat);
            }

            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
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

        while (ejecutando) {
            Mat frame = new Mat();
            if (camara.read(frame)) {
                procesarFrame(frame);
                mostrarFrame(frame);
            }
            frame.release();

            try {
                Thread.sleep(33);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (camara != null && camara.isOpened()) camara.release();
        if (frameAnterior != null) frameAnterior.release();
    }

    private void procesarFrame(Mat frame) {
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
        Imgproc.threshold(diff, diff, 25, 255, Imgproc.THRESH_BINARY);
        double movimiento = Core.sumElems(diff).val[0];

        if (movimiento > 500000) {
            Platform.runLater(() -> lblMensaje.setText("Analizando gesto..."));

            Mat reducido = new Mat();
            Imgproc.resize(gray, reducido, new Size(100, 100));
            String signo = detectarSigno(reducido);

            if (signo != null) {
                Platform.runLater(() -> lblMensaje.setText("Signo detectado: " + signo));
            } else {
                Platform.runLater(() -> lblMensaje.setText("No se reconoce el gesto"));
                reiniciar();
            }

            reducido.release();
        }

        frameAnterior.release();
        frameAnterior = gray.clone();

        diff.release();
        gray.release();
    }

    public String detectarSigno(Mat grayFrame) {
        Mat img = new Mat();
        Imgproc.resize(grayFrame, img, new Size(100, 100));

        double mejorError = Double.MAX_VALUE;
        String mejorSigno = null;

        for (Map.Entry<String, Mat> entry : signos.entrySet()) {
            Mat imgBD = entry.getValue();

            if (!img.size().equals(imgBD.size())) continue;

            Mat diff = new Mat();
            Core.absdiff(img, imgBD, diff);
            double error = Core.sumElems(diff).val[0];

            if (error < mejorError) {
                mejorError = error;
                mejorSigno = entry.getKey();
            }

            diff.release();
        }

        img.release();
        return (mejorError < 900000) ? mejorSigno : null;
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
    }

    public ControladorPersonasMudas reiniciar() {
        detener();
        if (camara != null && camara.isOpened()) camara.release();

        try {
            this.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Platform.runLater(() -> lblMensaje.setText(textoMensaje));

        try {
            session.beginTransaction();
            gDAO.borrar(session);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            HibernateUtil.commitTx(session);
        }

        ControladorPersonasMudas nuevo = new ControladorPersonasMudas(imageView, lblMensaje);
        nuevo.cargarSignosDesdeBD();
        nuevo.start();
        return nuevo;
    }
}