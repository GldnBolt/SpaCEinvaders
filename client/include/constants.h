#ifndef CONSTANTS_H
#define CONSTANTS_H

/* Conexion del servidor Java */
#define SERVER_IP "127.0.0.1"
#define SERVER_PORT 5000

/* Limites de datos que recibe el cliente desde el servidor */
#define MAX_PLAYERS 2
#define MAX_ALIENS 80
#define MAX_PROJECTILES 100
#define MAX_BUNKERS 4
#define MAX_NAME_LEN 32
#define MAX_MESSAGE_LEN 8192
#define MAX_DIRECTION_LEN 8

/* Coordenadas logicas usadas por el servidor */
#define WORLD_WIDTH 800
#define WORLD_HEIGHT 480

/* Tamano de la vista en consola */
#define SCREEN_WIDTH 100
#define SCREEN_HEIGHT 30

/* Controles */
#define KEY_LEFT 'a'
#define KEY_RIGHT 'd'
#define KEY_FIRE 'f'
#define KEY_QUIT 'q'

#endif
