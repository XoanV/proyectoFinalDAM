package controlador;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.types.TFloat32;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import org.opencv.imgproc.Moments;

public class modeloGestos {

    private final SavedModelBundle model;
    private final Session session;
    private final String[] labels = {"hola", "adios", "buenas_noches"};

    public modeloGestos(String modelPath) {
        model = SavedModelBundle.load(modelPath, "serve");
        session = model.session();
    }

    /**
     * Extrae keypoints de la mano usando OpenCV puro
     * @param frame imagen BGR
     * @return array de floats representando la mano
     */
    public float[] extraerKeypoints(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(7, 7), 0);

        Mat thresh = new Mat();
        Imgproc.threshold(gray, thresh, 60, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(thresh, contornos, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contornos.isEmpty()) return new float[30]; // fallback: mano no detectada

        // Contorno más grande = mano
        MatOfPoint mano = contornos.get(0);
        double maxArea = Imgproc.contourArea(mano);
        for (MatOfPoint c : contornos) {
            double area = Imgproc.contourArea(c);
            if (area > maxArea) {
                maxArea = area;
                mano = c;
            }
        }

        Moments m = Imgproc.moments(mano);
        int cx = (int) (m.m10 / m.m00);
        int cy = (int) (m.m01 / m.m00);

        // Bounding box
        Rect r = Imgproc.boundingRect(mano);

        float[] keypoints = new float[30];

        // 0-1: centroid
        keypoints[0] = cx / (float)frame.cols();
        keypoints[1] = cy / (float)frame.rows();

        // 2-5: bounding box normalizado
        keypoints[2] = r.x / (float)frame.cols();
        keypoints[3] = r.y / (float)frame.rows();
        keypoints[4] = (r.x + r.width) / (float)frame.cols();
        keypoints[5] = (r.y + r.height) / (float)frame.rows();

        // 6-29: puntos uniformes del contorno (normalizados)
        Point[] points = mano.toArray();
        int step = Math.max(1, points.length / 24); // para rellenar 24 valores
        for (int i = 0, k = 6; i < points.length && k < 30; i += step, k++) {
            keypoints[k] = (float) (points[i].x / (float)frame.cols()); // solo X por simplicidad
        }

        gray.release();
        thresh.release();
        return keypoints;
    }

    /**
     * Predice el gesto a partir de los keypoints
     * @param keypoints array de floats
     * @return índice del gesto (-1 si no reconoce)
     */
public int predecir(float[] keypoints) {
    try (TFloat32 input = TFloat32.tensorOf(org.tensorflow.ndarray.Shape.of(1, 1, keypoints.length))) {
        // Llenar tensor de entrada
        for (int j = 0; j < keypoints.length; j++) {
            input.setFloat(keypoints[j], 0, 0, j); // shape = [1,1,63]
        }

        // Ejecutar predicción
        try (TFloat32 output = (TFloat32) session.runner()
                .feed("serving_default_mobilenetv2_1.00_224_input:0", input)
                .fetch("StatefulPartitionedCall:0")
                .run()
                .get(0)) {

            // Buscar índice con máxima probabilidad
            float max = -1;
            int index = -1;
            for (int i = 0; i < labels.length; i++) {
                float v = output.getFloat(0, i);
                if (v > max) {
                    max = v;
                    index = i;
                }
            }
            return index;
        }
    } catch (Exception e) {
        e.printStackTrace();
        return -1;
    }
}

    /**
     * Devuelve la etiqueta del gesto
     */
    public String getLabel(int id) {
        if (id >= 0 && id < labels.length) return labels[id];
        return "desconocido";
    }

    /**
     * Cierra el modelo para liberar memoria
     */
    public void cerrarModelo() {
        if (model != null) model.close();
    }
}