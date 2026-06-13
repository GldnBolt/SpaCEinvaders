#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifdef _WIN32
#include <conio.h>
#include <windows.h>
#else
#include <unistd.h>
#include <termios.h>
#include <fcntl.h>
#endif

#include "constants.h"
#include "game.h"
#include "network.h"

#ifndef _WIN32
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
        int received = receive_line(sockfd, buffer, sizeof(buffer));

        if (received <= 0) {
            break;
        }

        if (parse_state_message(buffer, &state)) {
            render_game(&state, localPlayerId, role);
        } else if (strncmp(buffer, "ERROR|", 6) == 0) {
            printf("\nMensaje de error del servidor: %s\n", buffer);
        }

        if (key_pressed()) {
            int ch = read_key();

            if (ch == KEY_QUIT) {
                send_disconnect(sockfd, localPlayerId);
                break;
            }

            if (!isSpectator) {
                if (ch == KEY_LEFT) {
                    send_action(sockfd, localPlayerId, "MOVE_LEFT");
                } else if (ch == KEY_RIGHT) {
                    send_action(sockfd, localPlayerId, "MOVE_RIGHT");
                } else if (ch == KEY_FIRE) {
                    send_action(sockfd, localPlayerId, "FIRE");
                }
            }
        }
    }

    leave_game_screen();
    disable_raw_keyboard();
    close_socket(sockfd);
    cleanup_sockets();
    printf("Cliente C cerrado.\n");
    return 0;
}
