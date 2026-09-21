#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <winsock2.h>
#include <windows.h>
#include <conio.h>
#else
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#include <sys/select.h>
#endif

#include "constants.h"
#include "game.h"
#include "network.h"

#ifndef _WIN32


/*
 * Cliente de consola de spaCEinvaders.
 *
 * Este archivo contiene una versión base del cliente en C para conectarse
 * al servidor, recibir el estado del juego y enviar acciones del jugador
 * desde terminal. Fue utilizado como apoyo para probar la comunicación
 * cliente-servidor antes de integrar el cliente gráfico con raylib.
 *
 * La versión gráfica principal se encuentra en client/graphics/main_raylib.c.
 */


static struct termios originalTerminal;

static void enable_raw_keyboard(void) {
    struct termios raw;
    tcgetattr(STDIN_FILENO, &originalTerminal);
    raw = originalTerminal;
    raw.c_lflag &= (tcflag_t)~(ICANON | ECHO);
    tcsetattr(STDIN_FILENO, TCSANOW, &raw);
    fcntl(STDIN_FILENO, F_SETFL, fcntl(STDIN_FILENO, F_GETFL, 0) | O_NONBLOCK);
}

static void disable_raw_keyboard(void) {
    tcsetattr(STDIN_FILENO, TCSANOW, &originalTerminal);
}

static int key_pressed(void) {
    int ch = getchar();
    if (ch != EOF) {
        ungetc(ch, stdin);
        return 1;
    }
    return 0;
}

static int read_key(void) {
    return getchar();
}
#else
static void enable_raw_keyboard(void) { }
static void disable_raw_keyboard(void) { }
static int key_pressed(void) { return _kbhit(); }
static int read_key(void) { return _getch(); }
#endif

static void sleep_ms(int milliseconds) {
#ifdef _WIN32
    Sleep((DWORD)milliseconds);
#else
    usleep((useconds_t)milliseconds * 1000);
#endif
}

static int socket_has_data(int sockfd) {
    fd_set readfds;
    struct timeval timeout;

    FD_ZERO(&readfds);

#ifdef _WIN32
    FD_SET((SOCKET)sockfd, &readfds);
#else
    FD_SET(sockfd, &readfds);
#endif

    timeout.tv_sec = 0;
    timeout.tv_usec = 0;

#ifdef _WIN32
    return select(0, &readfds, NULL, NULL, &timeout) > 0;
#else
    return select(sockfd + 1, &readfds, NULL, NULL, &timeout) > 0;
#endif
}

#ifdef _WIN32
static void enable_ansi_console(void) {
    HANDLE output = GetStdHandle(STD_OUTPUT_HANDLE);
    DWORD mode = 0;

    SetConsoleOutputCP(CP_UTF8);

    if (GetConsoleMode(output, &mode)) {
        mode |= ENABLE_VIRTUAL_TERMINAL_PROCESSING;
        SetConsoleMode(output, mode);
    }
}
#else
static void enable_ansi_console(void) { }
#endif

static void enter_game_screen(void) {
    printf("\033[?1049h\033[2J\033[H\033[?25l");
    fflush(stdout);
}

static void leave_game_screen(void) {
    printf("\033[?25h\033[2J\033[H\033[?1049l");
    fflush(stdout);
}

static void send_action(int sockfd, int playerId, const char *command) {
    char message[128];
    snprintf(message, sizeof(message), "ACTION|playerId=%d|cmd=%s\n", playerId, command);
    send_message(sockfd, message);
}

static void send_disconnect(int sockfd, int playerId) {
    char message[128];
    snprintf(message, sizeof(message), "DISCONNECT|playerId=%d\n", playerId);
    send_message(sockfd, message);
}

int main(int argc, char *argv[]) {
    int sockfd;
    int localPlayerId = 0;
    int isSpectator = 0;
    char role[16] = "PLAYER";
    char buffer[MAX_MESSAGE_LEN];
    GameState state;

    if (argc >= 2 && strcmp(argv[1], "spectator") == 0) {
        isSpectator = 1;
        strcpy(role, "SPECTATOR");
    }

    init_game_state(&state);
    enable_ansi_console();

    if (init_sockets() != 0) {
        return 1;
    }

    sockfd = connect_to_server(SERVER_IP, SERVER_PORT);

    if (sockfd < 0) {
        printf("No se pudo conectar al servidor.\n");
        cleanup_sockets();
        return 1;
    }

    snprintf(buffer, sizeof(buffer), "HELLO|role=%s|name=ClienteC\n", role);
    send_message(sockfd, buffer);

    if (receive_line(sockfd, buffer, sizeof(buffer)) <= 0) {
        printf("No se recibio respuesta del servidor.\n");
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

    printf("Conectado al servidor como %s. playerId=%d\n", role, localPlayerId);
    printf("Esperando estado inicial...\n");

    enable_raw_keyboard();
    enter_game_screen();

    while (1) {
        if (key_pressed()) {
            int ch = read_key();

            if (ch == KEY_QUIT || ch == 'Q') {
                send_disconnect(sockfd, localPlayerId);
                break;
            }

            if (!isSpectator) {
                if (ch == KEY_LEFT || ch == 'A') {
                    send_action(sockfd, localPlayerId, "MOVE_LEFT");
                } else if (ch == KEY_RIGHT || ch == 'D') {
                    send_action(sockfd, localPlayerId, "MOVE_RIGHT");
                } else if (ch == KEY_FIRE || ch == 'F' || ch == ' ') {
                    send_action(sockfd, localPlayerId, "FIRE");
                }
            }
        }

        if (socket_has_data(sockfd)) {
            int received = receive_line(sockfd, buffer, sizeof(buffer));

            if (received <= 0) {
                break;
            }

            if (parse_state_message(buffer, &state)) {
                render_game(&state, localPlayerId, role);
            } else if (strncmp(buffer, "ERROR|", 6) == 0) {
                printf("\nMensaje de error del servidor: %s\n", buffer);
            }
        }

        sleep_ms(15);
    }

    leave_game_screen();
    disable_raw_keyboard();
    close_socket(sockfd);
    cleanup_sockets();

    printf("Cliente C cerrado.\n");
    return 0;
}