import cv2
import mediapipe as mp
import json
import random
import os

# Carpeta de salida
os.makedirs("dataset", exist_ok=True)
SALIDA = "dataset/todos_los_signos.jsonl"

imagenes = [
    ("./Signos/Saludo.JPG", "Hola"),
    ("./Signos/adios.JPG", "Adios"),
    ("./Signos/noches.JPG", "Buenas_noches")
]

mp_hands = mp.solutions.hands

def variar(lm):
    return {
        "x": min(max(lm.x + random.uniform(-0.02, 0.02), 0), 1),
        "y": min(max(lm.y + random.uniform(-0.02, 0.02), 0), 1),
        "z": lm.z
    }

with open(SALIDA, "w") as f:
    for imagen_path, etiqueta in imagenes:
        img = cv2.imread(imagen_path)
        if img is None:
            print(f"❌ No se encontró la imagen: {imagen_path}")
            continue

        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

        with mp_hands.Hands(static_image_mode=True, max_num_hands=1) as hands:
            results = hands.process(img_rgb)
            if not results.multi_hand_landmarks:
                print(f"❌ No se detectó mano en: {imagen_path}")
                continue

            base = results.multi_hand_landmarks[0]
            print(f"✔ Mano detectada en {imagen_path}")

            for i in range(100):
                puntos = []
                for lm in base.landmark:
                    v = variar(lm)
                    puntos.extend([v["x"], v["y"], v["z"]])

                linea = {"input": puntos, "output": etiqueta}
                f.write(json.dumps(linea) + "\n")

print(f"Archivo generado: {SALIDA}")
