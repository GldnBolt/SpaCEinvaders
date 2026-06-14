#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#ifdef _WIN32
#include <windows.h>
#endif
#include "game.h"


static void clear_screen(void) {
    printf("\033[H");
    fflush(stdout);
}

#define C_RESET   "\033[0m"
#define C_DIM     "\033[2m"
#define C_BOLD    "\033[1m"
#define C_RED     "\033[91m"
#define C_GREEN   "\033[92m"
#define C_YELLOW  "\033[93m"
#define C_BLUE    "\033[94m"
#define C_MAGENTA "\033[95m"
#define C_CYAN    "\033[96m"
#define C_WHITE   "\033[97m"

static const char *color_for_symbol(char symbol) {
    switch (symbol) {
        case 'W': return C_MAGENTA; /* alien de 40 pts */
        case 'M': return C_CYAN;    /* alien de 20 pts */
        case 'v': return C_GREEN;   /* alien de 10 pts */
        case 'A': return C_BOLD C_CYAN;
        case 'V': return C_BLUE;
        case 'U': return C_BOLD C_RED;
        case '^': return C_YELLOW;
        case '!': return C_RED;
        case '#': return C_GREEN;
        case '+': return C_YELLOW;
        case '.': return C_RED;
        case '|':
        case '-': return C_DIM C_WHITE;
        default: return C_RESET;
    }
}

static void print_colored_char(char symbol) {
    const char *color = color_for_symbol(symbol);
    if (symbol == ' ') {
        putchar(' ');
    } else {
        printf("%s%c%s", color, symbol, C_RESET);
    }
}

static int scale_x(int x) {
    int value = (x * (SCREEN_WIDTH - 1)) / WORLD_WIDTH;
    if (value < 0) return 0;
    if (value >= SCREEN_WIDTH) return SCREEN_WIDTH - 1;
    return value;
}

static int scale_y(int y) {
    int value = (y * (SCREEN_HEIGHT - 1)) / WORLD_HEIGHT;
    if (value < 0) return 0;
    if (value >= SCREEN_HEIGHT) return SCREEN_HEIGHT - 1;
    return value;
}

static const char *get_field(const char *line, const char *key, char *out, size_t outSize) {
    char pattern[64];
    const char *start;
    const char *end;
    size_t length;

    snprintf(pattern, sizeof(pattern), "|%s=", key);
    start = strstr(line, pattern);

    if (start == NULL) {
        if (strncmp(line, key, strlen(key)) == 0 && line[strlen(key)] == '=') {
            start = line + strlen(key) + 1;
        } else {
            if (outSize > 0) out[0] = '\0';
            return NULL;
        }
    } else {
        start += strlen(pattern);
    }

    end = strchr(start, '|');
    if (end == NULL) {
        end = line + strlen(line);
    }

    length = (size_t)(end - start);
    if (length >= outSize) {
        length = outSize - 1;
    }

    memcpy(out, start, length);
    out[length] = '\0';
    return out;
}

void init_game_state(GameState *state) {
    memset(state, 0, sizeof(GameState));
    strcpy(state->status, "UNKNOWN");
    strcpy(state->ufo.direction, "NONE");
}

int parse_welcome_player_id(const char *line) {
    char value[32];

    if (strncmp(line, "WELCOME|", 8) != 0) {
        return 0;
    }

    if (get_field(line, "playerId", value, sizeof(value)) == NULL) {
        return 0;
    }

    return atoi(value);
}

static void parse_players(const char *text, GameState *state) {
    char copy[MAX_MESSAGE_LEN];
    char *token;

    state->playerCount = 0;
    if (text == NULL || text[0] == '\0') return;

    snprintf(copy, sizeof(copy), "%s", text);
    token = strtok(copy, ";");

    while (token != NULL && state->playerCount < MAX_PLAYERS) {
        Player *player = &state->players[state->playerCount];
        int parsed = sscanf(token, "%d,%d,%d,%d,%d", &player->id, &player->x, &player->y, &player->lives, &player->score);
        if (parsed == 5) {
            snprintf(player->name, sizeof(player->name), "Jugador%d", player->id);
            state->playerCount++;
        }
        token = strtok(NULL, ";");
    }
}

static void parse_aliens(const char *text, GameState *state) {
    char copy[MAX_MESSAGE_LEN];
    char *token;

    state->alienCount = 0;
    if (text == NULL || text[0] == '\0') return;

    snprintf(copy, sizeof(copy), "%s", text);
    token = strtok(copy, ";");

    while (token != NULL && state->alienCount < MAX_ALIENS) {
        Alien *alien = &state->aliens[state->alienCount];
        int parsed = sscanf(token, "%d,%d,%d,%d,%d", &alien->id, &alien->x, &alien->y, &alien->points, &alien->alive);
        if (parsed == 5) {
            state->alienCount++;
        }
        token = strtok(NULL, ";");
    }
}

static void parse_bunkers(const char *text, GameState *state) {
    char copy[MAX_MESSAGE_LEN];
    char *token;

    state->bunkerCount = 0;
    if (text == NULL || text[0] == '\0') return;

    snprintf(copy, sizeof(copy), "%s", text);
    token = strtok(copy, ";");

    while (token != NULL && state->bunkerCount < MAX_BUNKERS) {
        Bunker *bunker = &state->bunkers[state->bunkerCount];
        int parsed = sscanf(token, "%d,%d,%d,%d", &bunker->id, &bunker->x, &bunker->y, &bunker->health);
        if (parsed == 4) {
            state->bunkerCount++;
        }
        token = strtok(NULL, ";");
    }
}

static void parse_ufo(const char *text, GameState *state) {
    if (text == NULL || text[0] == '\0') return;

    sscanf(text, "%d,%d,%d,%7[^,],%d",
           &state->ufo.active,
           &state->ufo.x,
           &state->ufo.y,
           state->ufo.direction,
           &state->ufo.points);
}

static void parse_projectiles(const char *text, Projectile projectiles[], int *count) {
    char copy[MAX_MESSAGE_LEN];
    char *token;

    *count = 0;
    if (text == NULL || text[0] == '\0') return;

    snprintf(copy, sizeof(copy), "%s", text);
    token = strtok(copy, ";");

    while (token != NULL && *count < MAX_PROJECTILES) {
        Projectile *projectile = &projectiles[*count];
        int parsed = sscanf(token, "%d,%d,%d", &projectile->id, &projectile->x, &projectile->y);
        if (parsed == 3) {
            projectile->active = 1;
            (*count)++;
        }
        token = strtok(NULL, ";");
    }
}

int parse_state_message(const char *line, GameState *state) {
    char value[MAX_MESSAGE_LEN];

    if (strncmp(line, "STATE|", 6) != 0) {
        return 0;
    }

    init_game_state(state);

    if (get_field(line, "tick", value, sizeof(value)) != NULL) state->tick = atoi(value);
    if (get_field(line, "status", value, sizeof(value)) != NULL) {
        strncpy(state->status, value, sizeof(state->status) - 1);
        state->status[sizeof(state->status) - 1] = '\0';
    }
    if (get_field(line, "speed", value, sizeof(value)) != NULL) state->speed = atoi(value);

    if (get_field(line, "players", value, sizeof(value)) != NULL) parse_players(value, state);
    if (get_field(line, "aliens", value, sizeof(value)) != NULL) parse_aliens(value, state);
    if (get_field(line, "bunkers", value, sizeof(value)) != NULL) parse_bunkers(value, state);
    if (get_field(line, "ufo", value, sizeof(value)) != NULL) parse_ufo(value, state);
    if (get_field(line, "playerShots", value, sizeof(value)) != NULL) parse_projectiles(value, state->playerShots, &state->playerShotCount);
    if (get_field(line, "enemyShots", value, sizeof(value)) != NULL) parse_projectiles(value, state->enemyShots, &state->enemyShotCount);

    return 1;
}

static void put_char(char board[SCREEN_HEIGHT][SCREEN_WIDTH + 1], int x, int y, char symbol) {
    if (x >= 0 && x < SCREEN_WIDTH && y >= 0 && y < SCREEN_HEIGHT) {
        board[y][x] = symbol;
    }
}

static char alien_symbol(int points) {
    if (points >= 40) return 'W';  /* pulpo / mayor puntaje */
    if (points >= 20) return 'M';  /* cangrejo */
    return 'v';                    /* calamar */
}

void render_game(const GameState *state, int localPlayerId, const char *role) {
    char board[SCREEN_HEIGHT][SCREEN_WIDTH + 1];
    int row;
    int col;

    for (row = 0; row < SCREEN_HEIGHT; row++) {
        for (col = 0; col < SCREEN_WIDTH; col++) {
            board[row][col] = ' ';
        }
        board[row][SCREEN_WIDTH] = '\0';
    }

    for (col = 0; col < SCREEN_WIDTH; col++) {
        board[0][col] = '-';
        board[SCREEN_HEIGHT - 1][col] = '-';
    }

    for (row = 1; row < SCREEN_HEIGHT - 1; row++) {
        board[row][0] = '|';
        board[row][SCREEN_WIDTH - 1] = '|';
    }

    for (int i = 0; i < state->alienCount; i++) {
        const Alien *alien = &state->aliens[i];
        if (alien->alive) {
            put_char(board, scale_x(alien->x), scale_y(alien->y), alien_symbol(alien->points));
        }
    }

    for (int i = 0; i < state->bunkerCount; i++) {
        const Bunker *bunker = &state->bunkers[i];
        char symbol = bunker->health > 70 ? '#' : (bunker->health > 35 ? '+' : (bunker->health > 0 ? '.' : ' '));
        int bx = scale_x(bunker->x);
        int by = scale_y(bunker->y);
        put_char(board, bx - 1, by, symbol);
        put_char(board, bx, by, symbol);
        put_char(board, bx + 1, by, symbol);
    }

    if (state->ufo.active) {
        put_char(board, scale_x(state->ufo.x), scale_y(state->ufo.y), 'U');
    }

    for (int i = 0; i < state->playerShotCount; i++) {
        put_char(board, scale_x(state->playerShots[i].x), scale_y(state->playerShots[i].y), '^');
    }

    for (int i = 0; i < state->enemyShotCount; i++) {
        put_char(board, scale_x(state->enemyShots[i].x), scale_y(state->enemyShots[i].y), '!');
    }

    for (int i = 0; i < state->playerCount; i++) {
        const Player *player = &state->players[i];
        char symbol = player->id == localPlayerId ? 'A' : 'V';
        put_char(board, scale_x(player->x), scale_y(player->y), symbol);
    }

    clear_screen();
    int alive = 0;
    for (int i = 0; i < state->alienCount; i++) {
        if (state->aliens[i].alive) alive++;
    }

    printf(C_BOLD C_CYAN "spaCEinvaders" C_RESET "  modo:%s  ID:%d  tick:%d  estado:%s  vel:%d  aliens:%d/%d\n",
           role, localPlayerId, state->tick, state->status, state->speed, alive, state->alienCount);

    printf(C_BOLD "Jugadores:" C_RESET " ");
    for (int i = 0; i < state->playerCount; i++) {
        printf("P%d V=%d S=%d  ", state->players[i].id, state->players[i].lives, state->players[i].score);
    }
    printf("| " C_BOLD "Bunkers:" C_RESET " ");
    for (int i = 0; i < state->bunkerCount; i++) {
        printf("B%d=%d%% ", state->bunkers[i].id, state->bunkers[i].health);
    }
    printf("\n");

    for (row = 0; row < SCREEN_HEIGHT; row++) {
        for (col = 0; col < SCREEN_WIDTH; col++) {
            print_colored_char(board[row][col]);
        }
        putchar('\n');
    }

    if (strcmp(role, "SPECTATOR") == 0) {
        printf(C_YELLOW "q" C_RESET "=salir | espectador | ");
    } else {
        printf(C_YELLOW "a" C_RESET "=izq  " C_YELLOW "d" C_RESET "=der  " C_YELLOW "f" C_RESET "=disparar  " C_YELLOW "q" C_RESET "=salir | ");
    }

    printf(C_BOLD C_CYAN "A" C_RESET "=tu canon  " C_BLUE "V" C_RESET "=otro  " C_GREEN "v" C_RESET "/" C_CYAN "M" C_RESET "/" C_MAGENTA "W" C_RESET "=aliens  " C_RED "U" C_RESET "=ovni  " C_YELLOW "^" C_RESET "=tiro  " C_RED "!" C_RESET "=enemigo\n");
    fflush(stdout);
}
