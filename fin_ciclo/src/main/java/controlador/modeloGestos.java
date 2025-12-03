package controlador;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.types.TFloat32;
import org.tensorflow.ndarray.Shape;

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
        }
    }

    public String getLabel(int id) {
        return labels[id];
    }

    public float[] extraerKeypoints(org.opencv.core.Mat frame) {
        return new float[30];
    }

    public void cerrarModelo() {
        if (model != null) model.close();
    }
}