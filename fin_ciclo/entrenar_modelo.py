import json
import numpy as np
import tensorflow as tf

DATASET_FILES = {
    "Hola": "dataset/Hola.jsonl",
    "Adios": "dataset/Adios.jsonl",
    "Buenas_noches": "dataset/Buenas_noches.jsonl"
}

LABELS = list(DATASET_FILES.keys())
LABEL_TO_ID = {label: i for i, label in enumerate(LABELS)}

def load_jsonl(path, label):
    """
    Carga el JSONL y devuelve una lista de (frames, features) por gesto
    """
    samples = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                print(f"Advertencia: línea inválida en {path}, se ignora")
                continue

            frames = []

            # Solo procesar manos
            for part in ["keypoints_right_hand", "keypoints_left_hand"]:
                keypoints = obj.get(part, [])
                frame_vec = []
                if isinstance(keypoints, list):
                    for p in keypoints:
                        if isinstance(p, dict):
                            x = p.get("x", 0.0)
                            y = p.get("y", 0.0)
                            frame_vec.extend([x, y])
                frames.append(frame_vec)

            # Concatenar mano derecha + izquierda para cada frame
            if all(frames):  # si ambos hands tienen keypoints
                sample_vec = np.concatenate(frames, axis=0)
                samples.append((sample_vec, LABEL_TO_ID[label]))
            else:
                print(f"Advertencia: keypoints de manos faltantes en {path}")

    return samples

all_vectors = []
all_labels = []

for label, file in DATASET_FILES.items():
    data = load_jsonl(file, label)
    for x, y in data:
        all_vectors.append(x)
        all_labels.append(y)

# Convertir a array y pad de secuencia
max_len = max(len(v) for v in all_vectors)
# Suponemos que cada gesto es un solo "frame" plano; si tienes secuencias reales, ajustar
all_vectors = np.array([np.pad(v, (0, max_len - len(v))) for v in all_vectors])
all_labels = np.array(all_labels)

# Si tuvieras secuencias de frames por gesto, reshape a (samples, timesteps, features)
# Aquí cada gesto es un "frame", así que añadimos dimensión de timesteps = 1
all_vectors = all_vectors[:, np.newaxis, :]  # shape: (num_samples, 1, features)

print("Shape X:", all_vectors.shape)
print("Shape Y:", all_labels.shape)

# Crear modelo LSTM
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(all_vectors.shape[1], all_vectors.shape[2])),
    tf.keras.layers.LSTM(128, return_sequences=False),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dense(len(LABELS), activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

# Entrenamiento con validación
model.fit(all_vectors, all_labels, epochs=20, batch_size=16, validation_split=0.2)

# Guardar modelo
export_path = "modelo/modelo_gesto"
model.save(f"{export_path}/modelo_signo.keras")
print("Modelo exportado en:", export_path)
