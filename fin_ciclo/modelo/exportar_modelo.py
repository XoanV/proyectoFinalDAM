import keras

keras_path = r"C:\Users\veiga\Desktop\Proyecto\proyectoFinalDAM\fin_ciclo\modelo\modelo_gesto\modelo_signo.keras"

export_path = "modelo/modelo_gesto/"

print("Cargando modelo .keras...")
model = keras.models.load_model(keras_path)

print("Exportando a TensorFlow SavedModel (.pb)...")
model.export(export_path)

print("SavedModel generado en:", export_path)
