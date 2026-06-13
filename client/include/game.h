#ifndef GAME_H
#define GAME_H

#include "game_structs.h"

void init_game_state(GameState *state);
int parse_welcome_player_id(const char *line);
int parse_state_message(const char *line, GameState *state);
void render_game(const GameState *state, int localPlayerId, const char *role);

#endif
