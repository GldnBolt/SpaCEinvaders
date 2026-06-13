#ifndef GAME_STRUCTS_H
#define GAME_STRUCTS_H

#include "constants.h"

typedef struct {
    int id;
    char name[MAX_NAME_LEN];
    int x;
    int y;
    int lives;
    int score;
} Player;

typedef struct {
    int id;
    int x;
    int y;
    int points;
    int alive;
} Alien;

typedef struct {
    int id;
    int x;
    int y;
    int health;
} Bunker;

typedef struct {
    int active;
    int x;
    int y;
    char direction[MAX_DIRECTION_LEN];
    int points;
} UFO;

typedef struct {
    int id;
    int x;
    int y;
    int active;
} Projectile;

typedef struct {
    int tick;
    char status[32];
    int speed;

    Player players[MAX_PLAYERS];
    int playerCount;

    Alien aliens[MAX_ALIENS];
    int alienCount;

    Bunker bunkers[MAX_BUNKERS];
    int bunkerCount;

    UFO ufo;

    Projectile playerShots[MAX_PROJECTILES];
    int playerShotCount;

    Projectile enemyShots[MAX_PROJECTILES];
    int enemyShotCount;
} GameState;

#endif
