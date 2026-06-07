#define BTN_MOVE 13
#define BTN_FIRE 19

unsigned long lastClickTime = 0;
int clickCount = 0;

const unsigned long doubleClickDelay = 220;

bool lastMoveState = HIGH;
bool lastFireState = HIGH;

void setup() {

    pinMode(BTN_MOVE, INPUT_PULLUP);
    pinMode(BTN_FIRE, INPUT_PULLUP);

    Serial.begin(115200);

    Serial.println("Sistema iniciado");
}

void loop() {

    // =========================
    // BOTON MOVIMIENTO
    // =========================

    bool moveState = digitalRead(BTN_MOVE);

    // Detectar presionado
    if (lastMoveState == HIGH && moveState == LOW) {

        delay(20);

        // confirmar que sigue presionado
        if (digitalRead(BTN_MOVE) == LOW) {

            clickCount++;

            if (clickCount == 1) {
                lastClickTime = millis();
            }

            // esperar a soltar
            while (digitalRead(BTN_MOVE) == LOW) {
                delay(1);
            }
        }
    }

    lastMoveState = moveState;

    // Revisar single o double click
    if (clickCount > 0 &&
        (millis() - lastClickTime) > doubleClickDelay) {

        if (clickCount == 1) {

            Serial.println("<---------LEFT");
        }
        else if (clickCount >= 2) {

            Serial.println("RIGHT-------->");
        }

        clickCount = 0;
    }

    // =========================
    // BOTON FIRE
    // =========================

    bool fireState = digitalRead(BTN_FIRE);

    if (lastFireState == HIGH && fireState == LOW) {

        delay(20);

        if (digitalRead(BTN_FIRE) == LOW) {

            Serial.println("FIRE***********");

            // esperar a soltar
            while (digitalRead(BTN_FIRE) == LOW) {
                delay(1);
            }
        }
    }

    lastFireState = fireState;
}