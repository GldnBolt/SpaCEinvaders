#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <windows.h>
#include "constants.h"
#include "game_structs.h"
#include "network.h"
#include <conio.h>

int main() {
    if(init_sockets() != 0) {
        return 1;
    }

    int sockfd = connect_to_server(SERVER_IP, SERVER_PORT);
    char buffer[4096];

    // Enviar HELLO
    snprintf(buffer, sizeof(buffer), "HELLO|role=PLAYER|name=JugadorC\n");
    send_message(sockfd, buffer);

    // Recibir WELCOME y primer STATE
    if(receive_message(sockfd, buffer, sizeof(buffer)) > 0)
        printf("%s\n", buffer);

    if(receive_message(sockfd, buffer, sizeof(buffer)) > 0)
        printf("%s\n", buffer);

    // Acciones de prueba
    snprintf(buffer, sizeof(buffer), "ACTION|playerId=0|cmd=MOVE_LEFT\n");
    send_message(sockfd, buffer);

    snprintf(buffer, sizeof(buffer), "ACTION|playerId=0|cmd=MOVE_RIGHT\n");
    send_message(sockfd, buffer);

    snprintf(buffer, sizeof(buffer), "ACTION|playerId=0|cmd=FIRE\n");
    send_message(sockfd, buffer);

    // Recibir algunos STATE
    char input[128];
    while(1) {
        // Leer mensajes del servidor
        if(receive_message(sockfd, buffer, sizeof(buffer)) > 0) {
            printf("%s\n", buffer);
        } else {
            printf("Servidor cerrado o desconectado.\n");
            break;
        }

        // Leer acciones del jugador desde teclado
        if(_kbhit()) {  // función de Windows para detectar tecla presionada
            char ch = _getch();
            if(ch == 'a') {
                snprintf(input, sizeof(input), "ACTION|playerId=0|cmd=MOVE_LEFT\n");
                send_message(sockfd, input);
            } else if(ch == 'd') {
                snprintf(input, sizeof(input), "ACTION|playerId=0|cmd=MOVE_RIGHT\n");
                send_message(sockfd, input);
            } else if(ch == 'f') {
                snprintf(input, sizeof(input), "ACTION|playerId=0|cmd=FIRE\n");
                send_message(sockfd, input);
            } else if(ch == 'q') {
                snprintf(input, sizeof(input), "DISCONNECT|playerId=0\n");
                send_message(sockfd, input);
                break;
            }
        }
    }

    close_socket(sockfd);
    cleanup_sockets();
    return 0;
}