import tensorflow as tf
import os

# ---------------- CONFIGURACIÓN ----------------
keras_path = r"C:\Users\veiga\Desktop\Proyecto\proyectoFinalDAM\fin_ciclo\modelo\modelo_gesto\modelo_signo.h5"

savedmodel_path = r"C:\Users\veiga\Desktop\Proyecto\proyectoFinalDAM\fin_ciclo\modelo\saved_model"
tflite_path = r"C:\Users\veiga\Desktop\Proyecto\proyectoFinalDAM\fin_ciclo\modelo\modelo_signo.tflite"
# ------------------------------------------------

# 1️⃣ Cargar modelo .h5
print("Cargando modelo .h5...")
model = tf.keras.models.load_model(keras_path)
print("Modelo cargado correctamente.")

# 2️⃣ Guardar como TensorFlow SavedModel
os.makedirs(savedmodel_path, exist_ok=True)
print(f"Guardando modelo como SavedModel en: {savedmodel_path}")
model.save(savedmodel_path, save_format="tf")
print("SavedModel guardado correctamente.")

# 3️⃣ Convertir a TensorFlow Lite
print("Convirtiendo a TensorFlow Lite (.tflite)...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
# Opcional: optimización para reducir tamaño y mejorar rendimiento
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Guardar TFLite
with open(tflite_path, "wb") as f:
    f.write(tflite_model)

print("TFLite guardado correctamente en:", tflite_path)
