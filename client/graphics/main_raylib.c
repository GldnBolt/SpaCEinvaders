#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

#include "raylib.h"
#include "constants.h"
#include "game.h"
#include "serial_uart.h"


/*
 * Cliente gráfico de spaCEinvaders.
 *
 * Este archivo contiene la interfaz gráfica del cliente en C utilizando raylib.
 * Se encarga de conectarse al servidor, recibir el estado del juego,
 * dibujarlo en pantalla y enviar las acciones del jugador.
 *
 * La lógica principal del juego se mantiene en el servidor Java.
 */



/*
   No incluimos windows.h, winsock2.h ni network.h aquí porque chocan con raylib.
   ya existen en client/src/network.c.
*/
int init_sockets(void);
int connect_to_server(const char *ip, int port);
int send_message(int sockfd, const char *message);
int receive_line(int sockfd, char *buffer, int bufferSize);
void close_socket(int sockfd);
void cleanup_sockets(void);
int socket_has_data(int sockfd);

#define WINDOW_WIDTH 1200
#define WINDOW_HEIGHT 760

#define PLAY_X 70
#define PLAY_Y 145
#define PLAY_W 1060
#define PLAY_H 500

static int world_to_screen_x(int worldX) {
    int margin = 55;
    int usableWidth = PLAY_W - (margin * 2);

    if (worldX < 0) {
        worldX = 0;
    }

    if (worldX > WORLD_WIDTH) {
        worldX = WORLD_WIDTH;
    }

    return PLAY_X + margin + (worldX * usableWidth) / WORLD_WIDTH;
}

static int world_to_screen_y(int worldY) {
    int margin = 45;
    int usableHeight = PLAY_H - (margin * 2);

    if (worldY < 0) {
        worldY = 0;
    }

    if (worldY > WORLD_HEIGHT) {
        worldY = WORLD_HEIGHT;
    }

    return PLAY_Y + margin + (worldY * usableHeight) / WORLD_HEIGHT;
}

static void send_action_graphic(int sockfd, int playerId, const char *command) {
    char message[128];
    snprintf(message, sizeof(message), "ACTION|playerId=%d|cmd=%s\n", playerId, command);
    send_message(sockfd, message);
}

static void send_disconnect_graphic(int sockfd, int playerId) {
    char message[128];
    snprintf(message, sizeof(message), "DISCONNECT|playerId=%d\n", playerId);
    send_message(sockfd, message);
}


static int equals_ignore_case(const char *a, const char *b) {
    while (*a != '\0' && *b != '\0') {
        if (toupper((unsigned char)*a) != toupper((unsigned char)*b)) {
            return 0;
        }

        a++;
        b++;
    }

    return *a == '\0' && *b == '\0';
}

static void handle_uart_command(int sockfd, int playerId, const char *command) {
    if (command == NULL || command[0] == '\0') {
        return;
    }

    if (equals_ignore_case(command, "L") ||
        equals_ignore_case(command, "LEFT") ||
        equals_ignore_case(command, "A") ||
        equals_ignore_case(command, "IZQ") ||
        equals_ignore_case(command, "IZQUIERDA")) {
        send_action_graphic(sockfd, playerId, "MOVE_LEFT");
        return;
    }

    if (equals_ignore_case(command, "R") ||
        equals_ignore_case(command, "RIGHT") ||
        equals_ignore_case(command, "D") ||
        equals_ignore_case(command, "DER") ||
        equals_ignore_case(command, "DERECHA")) {
        send_action_graphic(sockfd, playerId, "MOVE_RIGHT");
        return;
    }

    if (equals_ignore_case(command, "F") ||
        equals_ignore_case(command, "FIRE") ||
        equals_ignore_case(command, "SHOOT") ||
        equals_ignore_case(command, "DISPARAR")) {
        send_action_graphic(sockfd, playerId, "FIRE");
        return;
    }

    printf("Comando UART ignorado: %s\n", command);
}


static int count_alive_aliens(const GameState *state) {
    int alive = 0;

    for (int i = 0; i < state->alienCount; i++) {
        if (state->aliens[i].alive) {
            alive++;
        }
    }

    return alive;
}

static Player *find_local_player(GameState *state, int localPlayerId) {
    for (int i = 0; i < state->playerCount; i++) {
        if (state->players[i].id == localPlayerId) {
            return &state->players[i];
        }
    }

    return NULL;
}

static Color color_for_alien(int points) {
    if (points >= 40) {
        return MAGENTA;
    }

    if (points >= 20) {
        return SKYBLUE;
    }

    return GREEN;
}

static void draw_alien_sprite(int x, int y, int points) {
    Color color = color_for_alien(points);

    if (points >= 40) {
        DrawRectangle(x - 18, y - 10, 36, 20, color);
        DrawRectangle(x - 26, y - 2, 8, 10, color);
        DrawRectangle(x + 18, y - 2, 8, 10, color);
        DrawCircle(x - 8, y - 2, 3, BLACK);
        DrawCircle(x + 8, y - 2, 3, BLACK);
    } else if (points >= 20) {
        DrawCircle(x, y, 17, color);
        DrawRectangle(x - 23, y - 5, 8, 10, color);
        DrawRectangle(x + 15, y - 5, 8, 10, color);
        DrawCircle(x - 6, y - 3, 3, BLACK);
        DrawCircle(x + 6, y - 3, 3, BLACK);
    } else {
        DrawTriangle(
            (Vector2){x, y - 18},
            (Vector2){x - 20, y + 15},
            (Vector2){x + 20, y + 15},
            color
        );
        DrawCircle(x - 6, y + 2, 3, BLACK);
        DrawCircle(x + 6, y + 2, 3, BLACK);
    }
}

static void draw_bunker_sprite(int x, int y, int health) {
    Color color;

    if (health <= 0) {
        return;
    }

    if (health > 70) {
        color = GREEN;
    } else if (health > 35) {
        color = YELLOW;
    } else {
        color = ORANGE;
    }

    DrawRectangle(x - 38, y - 18, 76, 18, color);
    DrawRectangle(x - 50, y, 100, 22, color);
    DrawRectangle(x - 18, y + 8, 36, 20, BLACK);

    if (health < 70) {
        DrawRectangle(x - 42, y - 10, 18, 16, BLACK);
        DrawRectangle(x + 22, y + 2, 16, 16, BLACK);
    }

    if (health < 35) {
        DrawRectangle(x - 8, y - 18, 22, 14, BLACK);
        DrawRectangle(x + 38, y - 5, 16, 18, BLACK);
    }

    DrawText(TextFormat("%d%%", health), x - 18, y + 34, 14, color);
}

static void draw_player_sprite(int x, int y, int isLocalPlayer, int playerId) {
    Color color = isLocalPlayer ? SKYBLUE : BLUE;
    const char *label = isLocalPlayer ? "TU" : TextFormat("P%d", playerId);

    DrawTriangle(
        (Vector2){x, y - 28},
        (Vector2){x - 32, y + 25},
        (Vector2){x + 32, y + 25},
        color
    );

    DrawRectangle(x - 18, y + 8, 36, 16, color);
    DrawCircle(x, y - 5, 5, WHITE);

    DrawText(label, x - 12, y + 34, 16, color);
}

static void draw_ufo_sprite(int x, int y, int points) {
    DrawEllipse(x, y, 42, 16, RED);
    DrawCircle(x, y - 10, 18, ORANGE);
    DrawText(TextFormat("+%d", points), x - 18, y - 35, 16, YELLOW);
}

static void draw_projectiles(const GameState *state) {
    for (int i = 0; i < state->playerShotCount; i++) {
        int x = world_to_screen_x(state->playerShots[i].x);
        int y = world_to_screen_y(state->playerShots[i].y);

        DrawRectangle(x - 3, y - 14, 6, 18, YELLOW);
    }

    for (int i = 0; i < state->enemyShotCount; i++) {
        int x = world_to_screen_x(state->enemyShots[i].x);
        int y = world_to_screen_y(state->enemyShots[i].y);

        DrawRectangle(x - 3, y - 4, 6, 18, RED);
    }
}

static void draw_game(const GameState *state, int localPlayerId, const char *role) {
    Player *localPlayer;
    int aliveAliens = count_alive_aliens(state);

    ClearBackground(BLACK);

    
DrawText("spaCEinvaders", 70, 25, 38, SKYBLUE);

if (strcmp(role, "SPECTATOR") == 0) {
    DrawText("Rol: ESPECTADOR", 430, 28, 20, WHITE);
} else {
    DrawText(TextFormat("Rol: JUGADOR %d", localPlayerId), 430, 28, 20, WHITE);
}

DrawText(TextFormat("Estado: %s", state->status), 670, 28, 20, WHITE);
DrawText(TextFormat("Velocidad: %d", state->speed), 910, 28, 20, WHITE);
localPlayer = find_local_player((GameState *)state, localPlayerId);

if (localPlayer != NULL) {
    DrawText(TextFormat("Vidas: %d", localPlayer->lives), 70, 82, 22, GREEN);
    DrawText(TextFormat("Score: %d", localPlayer->score), 210, 82, 22, YELLOW);
} else {
    DrawText("Vidas: -", 70, 82, 22, GREEN);
    DrawText("Score: -", 210, 82, 22, YELLOW);
}

DrawText(TextFormat("Aliens: %d/%d", aliveAliens, state->alienCount), 370, 82, 22, MAGENTA);

DrawText("Bunkers:", 560, 82, 22, WHITE);

for (int i = 0; i < state->bunkerCount; i++) {
    Color bunkerColor = state->bunkers[i].health > 35 ? GREEN : ORANGE;

    DrawText(
        TextFormat("B%d=%d%%", state->bunkers[i].id, state->bunkers[i].health),
        680 + i * 115,
        84,
        18,
        bunkerColor
    );
}


    DrawRectangleLines(PLAY_X, PLAY_Y, PLAY_W, PLAY_H, DARKGRAY);
    DrawRectangleLines(PLAY_X + 2, PLAY_Y + 2, PLAY_W - 4, PLAY_H - 4, GRAY);
    BeginScissorMode(PLAY_X + 3, PLAY_Y + 3, PLAY_W - 6, PLAY_H - 6);

    for (int i = 0; i < state->alienCount; i++) {
        const Alien *alien = &state->aliens[i];

        if (alien->alive) {
            draw_alien_sprite(
                world_to_screen_x(alien->x),
                world_to_screen_y(alien->y),
                alien->points
            );
        }
    }

    for (int i = 0; i < state->bunkerCount; i++) {
        const Bunker *bunker = &state->bunkers[i];

        draw_bunker_sprite(
            world_to_screen_x(bunker->x),
            world_to_screen_y(bunker->y),
            bunker->health
        );
    }

    if (state->ufo.active) {
        draw_ufo_sprite(
            world_to_screen_x(state->ufo.x),
            world_to_screen_y(state->ufo.y),
            state->ufo.points
        );
    }

    draw_projectiles(state);

    for (int i = 0; i < state->playerCount; i++) {
    const Player *player = &state->players[i];

    draw_player_sprite(
        world_to_screen_x(player->x),
        world_to_screen_y(player->y),
        player->id == localPlayerId,
        player->id
    );
    }
EndScissorMode();

    if (strcmp(role, "SPECTATOR") == 0) {
        DrawText("ESPECTADOR: observa la partida | Q o ESC para salir", 320, 685, 22, YELLOW);
    } else {
        DrawText("A / Flecha izquierda = mover izquierda    D / Flecha derecha = mover derecha", 95, 675, 20, YELLOW);
	DrawText("F / Espacio = disparar    Q / ESC = salir", 350, 705, 20, YELLOW);
    }
}

int main(int argc, char *argv[]) {
    int sockfd;
    int localPlayerId = 0;
    int isSpectator = 0;
    int useUart = 0;
    int uartBaud = 115200;
    char uartPort[64] = "";
    SerialPort uart = SERIAL_INVALID;
    char role[16] = "PLAYER";
    char buffer[MAX_MESSAGE_LEN];
    GameState state;

    for (int i = 1; i < argc; i++) {
    	if (strcmp(argv[i], "spectator") == 0) {
            isSpectator = 1;
            strcpy(role, "SPECTATOR");
        } else if (strcmp(argv[i], "uart") == 0 && i + 1 < argc) {
            useUart = 1;
            snprintf(uartPort, sizeof(uartPort), "%s", argv[i + 1]);
            i++;
        } else if (strcmp(argv[i], "baud") == 0 && i + 1 < argc) {
            uartBaud = atoi(argv[i + 1]);
            i++;
        }
    }

    init_game_state(&state);

    if (init_sockets() != 0) {
        printf("No se pudieron iniciar sockets.\n");
        return 1;
    }

    sockfd = connect_to_server(SERVER_IP, SERVER_PORT);

    if (sockfd < 0) {
        printf("No se pudo conectar al servidor Java.\n");
        cleanup_sockets();
        return 1;
    }

    snprintf(buffer, sizeof(buffer), "HELLO|role=%s|name=ClienteGraficoC\n", role);
    send_message(sockfd, buffer);

    if (receive_line(sockfd, buffer, sizeof(buffer)) <= 0) {
        printf("No se recibio WELCOME del servidor.\n");
        close_socket(sockfd);
        cleanup_sockets();
        return 1;
    }

    if (strncmp(buffer, "ERROR|", 6) == 0) {
        printf("Servidor rechazo la conexion:\n%s\n", buffer);
        close_socket(sockfd);
        cleanup_sockets();
        return 1;
    }

    localPlayerId = parse_welcome_player_id(buffer);

    if (useUart && !isSpectator) {
        uart = serial_open(uartPort, uartBaud);

    	if (uart == SERIAL_INVALID) {
            printf("No se pudo abrir el puerto UART %s a %d baudios.\n", uartPort, uartBaud);
            close_socket(sockfd);
            cleanup_sockets();
            return 1;
   	}

        printf("Control fisico UART activo en %s a %d baudios.\n", uartPort, uartBaud);
        }

    InitWindow(WINDOW_WIDTH, WINDOW_HEIGHT, "spaCEinvaders - Cliente grafico C");
    SetTargetFPS(30);

    while (!WindowShouldClose()) {
    if (IsKeyPressed(KEY_Q) || IsKeyPressed(KEY_ESCAPE)) {
        send_disconnect_graphic(sockfd, localPlayerId);
        break;
    }

    if (!isSpectator) {
        if (IsKeyDown(KEY_A) || IsKeyDown(KEY_LEFT)) {
            send_action_graphic(sockfd, localPlayerId, "MOVE_LEFT");
        }

        if (IsKeyDown(KEY_D) || IsKeyDown(KEY_RIGHT)) {
            send_action_graphic(sockfd, localPlayerId, "MOVE_RIGHT");
        }

        if (IsKeyPressed(KEY_F) || IsKeyPressed(KEY_SPACE)) {
            send_action_graphic(sockfd, localPlayerId, "FIRE");
        }
    }


    if (!isSpectator && uart != SERIAL_INVALID) {
        char uartCommand[64];

        while (serial_read_command(uart, uartCommand, sizeof(uartCommand))) {
            handle_uart_command(sockfd, localPlayerId, uartCommand);
        }
    }

    while (socket_has_data(sockfd)) {
        int received = receive_line(sockfd, buffer, sizeof(buffer));

        if (received <= 0) {
            send_disconnect_graphic(sockfd, localPlayerId);
            CloseWindow();
            close_socket(sockfd);
            cleanup_sockets();
            return 0;
        }

        if (parse_state_message(buffer, &state)) {
            /* Estado actualizado correctamente */
        } else if (strncmp(buffer, "ERROR|", 6) == 0) {
            printf("Mensaje del servidor: %s\n", buffer);
        }
    }

    BeginDrawing();
    draw_game(&state, localPlayerId, role);

    {
        Player *localPlayer = find_local_player(&state, localPlayerId);

        if (!isSpectator && localPlayer != NULL && localPlayer->lives <= 0) {
            DrawRectangle(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT, Fade(BLACK, 0.75f));
            DrawText("GAME OVER", WINDOW_WIDTH / 2 - 150, WINDOW_HEIGHT / 2 - 60, 50, RED);
            DrawText("Presiona Q o ESC para salir", WINDOW_WIDTH / 2 - 190, WINDOW_HEIGHT / 2 + 10, 28, YELLOW);
        }
    }

    EndDrawing();
}

    if (uart != SERIAL_INVALID) {
        serial_close(uart);
    }
    
    CloseWindow();
    close_socket(sockfd);
    cleanup_sockets();

    return 0;
}