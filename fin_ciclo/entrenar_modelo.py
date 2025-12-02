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
    samples = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            try:
                obj = json.loads(line)
            except json.JSONDecodeError:
                print(f"Advertencia: línea inválida en {path}, se ignora")
                continue

            vec = []

            # Procesar partes del cuerpo de manera segura
            for part in ["keypoints_body", "keypoints_right_hand", "keypoints_left_hand"]:
                keypoints = obj.get(part, [])
                if isinstance(keypoints, list):
                    for p in keypoints:
                        if isinstance(p, dict):
                            x = p.get("x", 0.0)
                            y = p.get("y", 0.0)
                            vec.extend([x, y])

            if vec:  # solo agregar si hay datos
                samples.append((np.array(vec, dtype=np.float32), LABEL_TO_ID[label]))
            else:
                print(f"Advertencia: ningún keypoint válido en línea de {path}")

    return samples

all_vectors = []
all_labels = []

for label, file in DATASET_FILES.items():
    data = load_jsonl(file, label)
    for x, y in data:
        all_vectors.append(x)
        all_labels.append(y)

max_len = max(len(v) for v in all_vectors)
all_vectors = np.array([np.pad(v, (0, max_len - len(v))) for v in all_vectors])
all_labels = np.array(all_labels)

print("Shape X:", all_vectors.shape)
print("Shape Y:", all_labels.shape)

# Crear modelo
model = tf.keras.Sequential([
    tf.keras.layers.Input(shape=(max_len,)),
    tf.keras.layers.Dense(128, activation="relu"),
    tf.keras.layers.Dense(64, activation="relu"),
    tf.keras.layers.Dense(len(LABELS), activation="softmax")
])

model.compile(
    optimizer="adam",
    loss="sparse_categorical_crossentropy",
    metrics=["accuracy"]
)

model.fit(all_vectors, all_labels, epochs=20, batch_size=16)

export_path = "modelo/modelo_gesto"
model.save("modelo/modelo_gesto/modelo_signo.keras")
print("Modelo exportado en:", export_path)