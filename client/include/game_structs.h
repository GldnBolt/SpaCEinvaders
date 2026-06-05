#ifndef GAME_STRUCTS_H
#define GAME_STRUCTS_H

#include "constants.h"

typedef struct {
    int id;
    char name[MAX_NAME_LEN];
    int x, y;
    int lives;
    int score;
} Player;

typedef struct {
    int id;
    int x, y;
    int points;
    int alive;
} Alien;

typedef struct {
    int id;
    int x, y;
    int health;
} Bunker;

typedef struct {
    int id;
    int x, y;
    int active;
    char direction[4];
    int points;
} UFO;

typedef struct {
    int id;
    int ownerId;
    int x, y;
    int active;
    int fromPlayer;
} Projectile;

#endif