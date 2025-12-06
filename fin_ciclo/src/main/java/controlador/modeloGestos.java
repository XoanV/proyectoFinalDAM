package controlador;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.types.TFloat32;
import org.tensorflow.ndarray.Shape;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import org.opencv.imgproc.Moments;

public class modeloGestos {

    private final SavedModelBundle model;
    private final Session session;
    private final String[] labels = {"Hola", "Adios", "Buenas_noches"};

    public modeloGestos(String modelPath) {
        model = SavedModelBundle.load(modelPath, "serve");
        session = model.session();
    }

    public int predecir(float[] keypoints) {
        try (TFloat32 input = TFloat32.tensorOf(Shape.of(1, keypoints.length))) {

            for (int j = 0; j < keypoints.length; j++) {
                input.setFloat(keypoints[j], 0, j);
            }

            try (TFloat32 output = (TFloat32) session.runner()
                    .feed("serve_input_layer", input)
                    .fetch("StatefulPartitionedCall")
                    .run()
                    .get(0)) {

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
            return -1;
        }
    }

    public String getLabel(int id) {
        return labels[id];
    }
    public float[] extraerKeypoints(Mat frame) {
        Mat gray = new Mat();
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray, gray, new Size(7, 7), 0);

        Mat thresh = new Mat();
        Imgproc.threshold(gray, thresh, 60, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(thresh, contornos, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contornos.size() == 0) {
            gray.release();
            thresh.release();
            return new float[30];
        }

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

        if (m.m00 == 0) {
            gray.release();
            thresh.release();
            return new float[30];
        }

        int cx = (int) (m.m10 / m.m00);
        int cy = (int) (m.m01 / m.m00);

        float[] keypoints = new float[30];
        for (int i = 0; i < keypoints.length; i += 2) {
            keypoints[i] = cx + (float) (Math.random() * 20 - 10);  
            keypoints[i + 1] = cy + (float) (Math.random() * 20 - 10); 
        }

        gray.release();
        thresh.release();
        return keypoints;
    }

    public void cerrarModelo() {
        if (model != null) model.close();
    }
}