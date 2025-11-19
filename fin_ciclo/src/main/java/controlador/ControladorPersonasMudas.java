package controlador;

import controlador.factory.HibernateUtil;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
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
import modelo.vo.GestosPersonas;
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
    private final Map<Integer, Mat> signosBD = new HashMap<>();
    private final Map<Integer, String> significados = new HashMap<>();

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
        if (session != null && session.isOpen())
            session.close();
    }

    public void cargarSignosDesdeBD() {
        signosBD.clear();
        significados.clear();
        try {
            session.beginTransaction();
            List<Gestos> lista = gDAO.listarGestos(session);

            for (Gestos g : lista) {
                byte[] data = g.getImagen();
                Mat mat = Imgcodecs.imdecode(new MatOfByte(data), Imgcodecs.IMREAD_GRAYSCALE);
                Imgproc.resize(mat, mat, new Size(100, 100));
                signosBD.put(g.getId(), mat);
                significados.put(g.getId(), g.getSignificado());
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

            try { Thread.sleep(33); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
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
        Imgproc.threshold(diff, diff, 20, 255, Imgproc.THRESH_BINARY);
        double movimiento = Core.sumElems(diff).val[0];

        if (movimiento > 500000) {
            framesSinMovimiento = 0;

            if (!gestodetectado) {
                gestodetectado = true;
                Platform.runLater(() -> lblMensaje.setText("Analizando..."));

                Mat reducido = new Mat();
                Imgproc.resize(gray, reducido, new Size(100, 100));
                idSignoActual = detectarSigno(reducido);
                reducido.release();
            }

            if (gestodetectado && idSignoActual != -1) {
                guardarGestoPersona(frame, idSignoActual);
                String significado = significados.get(idSignoActual);
                Platform.runLater(() -> lblMensaje.setText("Signo detectado: " + significado + " (ID: " + idSignoActual + ")"));
            }

        } else {
            framesSinMovimiento++;
            if (framesSinMovimiento > 10 && gestodetectado) {
                gestodetectado = false;
                idSignoActual = -1;
            }
        }

        frameAnterior.release();
        frameAnterior = gray.clone();
        diff.release();
        gray.release();
    }

    private int detectarSigno(Mat frameReducido) {
        double mejorError = Double.MAX_VALUE;
        int mejorId = -1;

        for (Map.Entry<Integer, Mat> entry : signosBD.entrySet()) {
            Mat matBD = entry.getValue();
            if (!frameReducido.size().equals(matBD.size())) continue;

            Mat diff = new Mat();
            Core.absdiff(frameReducido, matBD, diff);
            double error = Core.sumElems(diff).val[0];
            if (error < mejorError) {
                mejorError = error;
                mejorId = entry.getKey();
            }
            diff.release();
        }

        return (mejorError < 900000) ? mejorId : -1;
    }

    private void guardarGestoPersona(Mat frame, int idGesto) {
        try {
            String nombreArchivo = "C:\\ProgramData\\MySQL\\MySQL Server 8.0\\Uploads\\frame_" + idGesto + "_" + System.currentTimeMillis() + ".jpg";
            Imgcodecs.imwrite(nombreArchivo, frame);

            session.beginTransaction();
            GestosPersonas gp = new GestosPersonas();
            gp.setImagenPersona(frameToBytes(frame));
            session.save(gp);
            session.getTransaction().commit();
        } catch (Exception e) {
            session.getTransaction().rollback();
            e.printStackTrace();
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

    public void detener() { ejecutando = false; }

    public ControladorPersonasMudas reiniciar() {
        detener();

        try {
            this.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Platform.runLater(() -> lblMensaje.setText(textoMensaje));

        ControladorPersonasMudas nuevo = new ControladorPersonasMudas(imageView, lblMensaje);
        nuevo.start();

        return nuevo;
    }
}
