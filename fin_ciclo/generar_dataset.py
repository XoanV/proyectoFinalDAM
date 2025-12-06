import cv2
import mediapipe as mp
from pathlib import Path

def create_data():
    sign = input('Which sign would you like to add? -> ')
    Path(f'dataset/{sign}').mkdir(parents=True, exist_ok=True)

    cap = cv2.VideoCapture(0)
    hands = mp.solutions.hands.Hands(static_image_mode=True, max_num_hands=1)
    i = 0

    while True:
        ret, frame = cap.read()
        if not ret:
            break

        rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = hands.process(rgb)

        if results.multi_hand_landmarks:
            h, w, _ = frame.shape
            landmarks = results.multi_hand_landmarks[0].landmark

            x_min = int(min(l.x for l in landmarks) * w) - 20
            x_max = int(max(l.x for l in landmarks) * w) + 20
            y_min = int(min(l.y for l in landmarks) * h) - 20
            y_max = int(max(l.y for l in landmarks) * h) + 20

            # Limitar a los bordes
            x_min = max(0, x_min)
            y_min = max(0, y_min)
            x_max = min(w, x_max)
            y_max = min(h, y_max)

            hand_img = frame[y_min:y_max, x_min:x_max]

            if hand_img.size > 0:
                i += 1
                cv2.imwrite(f'dataset/{sign}/{i}.png', hand_img)

        cv2.imshow("frame", frame)

        # Mantener ventana activa
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break

        # Parar tras 50 imágenes
        if i > 50:
            break

    cap.release()
    cv2.destroyAllWindows()

if __name__ == "__main__":
    create_data()
