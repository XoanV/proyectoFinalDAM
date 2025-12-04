from PIL import Image
import mediapipe as mp
import json
import numpy as np
import os
import random

# Carpeta donde están tus imágenes
IMAGES_PATH = "./Signos/"

# Diccionario de signos con sus imágenes
signos = {
    "hola": "Saludo.JPG",
    "adios": "adios.JPG",
    "buenas_noches": "noches.JPG"
}

mp_hands = mp.solutions.hands
mp_drawing = mp.solutions.drawing_utils

# Crear carpeta de salida
if not os.path.exists("dataset"):
    os.makedirs("dataset")

def aplicar_variacion(coord):
    # Variación pequeña en x e y (simula movimiento de mano)
    delta_x = random.uniform(-0.02, 0.02)
    delta_y = random.uniform(-0.02, 0.02)
    return {
        "x": min(max(coord.x + delta_x, 0.0), 1.0),
        "y": min(max(coord.y + delta_y, 0.0), 1.0)
    }

for signo, archivo in signos.items():
    image_path = os.path.join(IMAGES_PATH, archivo)
    image = cv2.imread(image_path)
    if image is None:
        print(f"No se encontró la imagen {image_path}")
        continue

    image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)

    with mp_hands.Hands(static_image_mode=True, max_num_hands=2, min_detection_confidence=0.5) as hands:
        for i in range(100):  # 100 frames por signo
            results = hands.process(image_rgb)

            if not results.multi_hand_landmarks:
                continue  # Si no detecta manos, salta

            keypoints_right_hand = []
            keypoints_left_hand = []

            for hand_landmarks, handedness in zip(results.multi_hand_landmarks, results.multi_handedness):
                hand_type = handedness.classification[0].label  # "Right" o "Left"
                keypoints = [aplicar_variacion(lm) for lm in hand_landmarks.landmark]

                # Guardar solo x, y
                keypoints_xy = [{"x": kp["x"], "y": kp["y"]} for kp in keypoints]

                if hand_type == "Right":
                    keypoints_right_hand = keypoints_xy
                else:
                    keypoints_left_hand = keypoints_xy

            # Guardar JSON por frame
            frame_data = {
                "keypoints_right_hand": keypoints_right_hand,
                "keypoints_left_hand": keypoints_left_hand
            }

            output_file = f"dataset/{signo}.json"
            with open(output_file, "w") as f:
                json.dump(frame_data, f, indent=4)

    print(f"Generados 100 frames para '{signo}'")


