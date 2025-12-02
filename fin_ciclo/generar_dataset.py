import json
import random
import os

# Crear carpeta dataset si no existe
os.makedirs("dataset", exist_ok=True)

DATASET_FILES = {
    "Hola": "dataset/Hola.jsonl",
    "Adios": "dataset/Adios.jsonl",
    "Buenas_noches": "dataset/Buenas_noches.jsonl"
}

NUM_SAMPLES = 50  # número de líneas por archivo
NUM_BODY_KEYPOINTS = 5
NUM_HAND_KEYPOINTS = 5

def generar_keypoints(n):
    return [{"x": round(random.random(), 3), "y": round(random.random(), 3)} for _ in range(n)]

for label, path in DATASET_FILES.items():
    with open(path, "w", encoding="utf-8") as f:
        for _ in range(NUM_SAMPLES):
            obj = {
                "keypoints_body": generar_keypoints(NUM_BODY_KEYPOINTS),
                "keypoints_right_hand": generar_keypoints(NUM_HAND_KEYPOINTS),
                "keypoints_left_hand": generar_keypoints(NUM_HAND_KEYPOINTS)
            }
            f.write(json.dumps(obj) + "\n")

print("✅ Datasets sintéticos generados correctamente en la carpeta 'dataset'.")

