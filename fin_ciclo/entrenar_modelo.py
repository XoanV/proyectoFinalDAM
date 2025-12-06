import json
import numpy as np
import tensorflow as tf
import os

DATASET_FILE = "dataset/todos_los_signos.jsonl"
LABELS = ["Hola", "Adios", "Buenas_noches"]
LABEL_TO_ID = {label: i for i, label in enumerate(LABELS)}

def load_jsonl(path):
    samples = []
    labels = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            obj = json.loads(line)
            keypoints = obj.get("input", [])
            if keypoints:
                samples.append(np.array(keypoints, dtype=np.float32))
                labels.append(LABEL_TO_ID[obj["output"]])
    return samples, labels

all_vectors, all_labels = load_jsonl(DATASET_FILE)
max_len = max(len(v) for v in all_vectors)
all_vectors = np.array([np.pad(v, (0, max_len - len(v))) for v in all_vectors])
all_labels = np.array(all_labels)
all_vectors = all_vectors[:, np.newaxis, :]

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

model.fit(all_vectors, all_labels, epochs=20, batch_size=16, validation_split=0.2)

export_path = "modelo/modelo_gesto"
os.makedirs(export_path, exist_ok=True)
model.save(f"{export_path}/modelo_signo.keras")