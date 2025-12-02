package controlador;

import org.tensorflow.SavedModelBundle;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class modeloGestos {

    private final SavedModelBundle model;
    private final Session session;
    private final String[] labels = {"Hola", "Adios", "Buenas_noches"};

    public modeloGestos(String modelPath) {
        // Carga el SavedModel exportado desde Python
        model = SavedModelBundle.load(modelPath, "serve");
        session = model.session();
    }

    public int predecir(float[] keypoints) {
        // TensorFlow 0.5.0 requiere un batch: float[1][n]
        float[][] inputBatch = new float[1][keypoints.length];
        inputBatch[0] = keypoints;

        // Crear tensor de entrada
        Tensor<Float> inputTensor = Tensor.create(inputBatch, Float.class);

        // Ejecutar el modelo
        Tensor<Float> outputTensor = session.runner()
                .feed("serving_default_input_1:0", inputTensor)  // Ajusta según tu SavedModel
                .fetch("StatefulPartitionedCall:0")            // Ajusta según tu SavedModel
                .run()
                .get(0)
                .expect(Float.class);

        // Copiar resultados
        float[][] probs = new float[1][labels.length];
        outputTensor.copyTo(probs);

        // Encontrar índice con mayor probabilidad
        int maxIdx = 0;
        float maxVal = probs[0][0];
        for (int i = 1; i < labels.length; i++) {
            if (probs[0][i] > maxVal) {
                maxVal = probs[0][i];
                maxIdx = i;
            }
        }

        inputTensor.close();
        outputTensor.close();

        return maxIdx;
    }

    public String getLabel(int id) {
        return labels[id];
    }

    // Método dummy de extracción de keypoints (rellénalo con tu lógica real)
    public float[] extraerKeypoints(org.opencv.core.Mat frame) {
        return new float[50]; // ejemplo, reemplaza con tus keypoints reales
    }
}

