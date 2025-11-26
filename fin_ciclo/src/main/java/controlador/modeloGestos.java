package controlador;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Optional;

import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.SavedModelBundle;
import org.tensorflow.Tensor;
import org.tensorflow.Operation;

public class modeloGestos {

    private final SavedModelBundle model;
    private final String inputName;
    private final String outputName;
    private final int inputWidth;
    private final int inputHeight;

    public modeloGestos(String rutaModelo, int width, int height) {
        this.inputWidth = width;
        this.inputHeight = height;

        model = SavedModelBundle.load(rutaModelo, "serve");

        // Detectar automáticamente input y output
        Optional<Operation> inputOp = model.session().graph().operations()
                .filter(op -> op.name().toLowerCase().contains("input"))
                .findFirst();

        Optional<Operation> outputOp = model.session().graph().operations()
                .filter(op -> op.name().toLowerCase().contains("stateful"))
                .findFirst();

        if (!inputOp.isPresent() || !outputOp.isPresent()) {
            throw new RuntimeException("No se encontraron operaciones válidas de input/output en el modelo.");
        }

        this.inputName = inputOp.get().name() + ":0";
        this.outputName = outputOp.get().name() + ":0";
    }

    public int predecirGesto(Mat frame) {
        if (frame.empty()) return -1;
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(rgb, rgb, new Size(inputWidth, inputHeight));

        int w = rgb.cols();
        int h = rgb.rows();
        float[] data = new float[w * h * 3];
        int idx = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double[] px = rgb.get(y, x);
                if (px == null) continue;
                data[idx++] = (float)(px[0]/255.0);
                data[idx++] = (float)(px[1]/255.0);
                data[idx++] = (float)(px[2]/255.0);
            }
        }
        rgb.release();

        try (Tensor<Float> input = Tensor.create(new long[]{1,h,w,3}, FloatBuffer.wrap(data))) {

            List<Tensor<?>> outputs = model.session().runner()
                    .feed(inputName, input)
                    .fetch(outputName)
                    .run();

            if (outputs.isEmpty()) return -1;

            Tensor<?> output = outputs.get(0);

            long[] shape = output.shape();
            if (shape.length != 2) {
                output.close();
                return -1;
            }

            float[][] resultados = new float[1][(int)shape[1]];
            output.copyTo(resultados);
            output.close();

            int maxIdx = 0;
            for (int i = 1; i < resultados[0].length; i++) {
                if (resultados[0][i] > resultados[0][maxIdx]) maxIdx = i;
            }

            return maxIdx;

        } catch(Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void cerrar() {
        if (model != null) model.close();
    }
}