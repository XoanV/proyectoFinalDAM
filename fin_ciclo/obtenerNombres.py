import tensorflow as tf

# Ruta a tu carpeta saved_model
model_path = "C:/Users/veiga/Desktop/Proyecto/proyectoFinalDAM/fin_ciclo/modelo/modelo/"

# Cargar el modelo
model = tf.saved_model.load(model_path)

# Obtener la función de inferencia (firma por defecto)
infer = model.signatures.get("serving_default")
if infer is None:
    raise ValueError("No se encontró la firma 'serving_default' en el modelo.")

# Mostrar los tensores de entrada
print("=== Entradas ===")
for name, tensor in infer.structured_input_signature[1].items():
    print(f"{name} -> {tensor}")

# Mostrar los tensores de salida
print("\n=== Salidas ===")
for name, tensor in infer.structured_outputs.items():
    print(f"{name} -> {tensor}")

