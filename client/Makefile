CC=gcc
CFLAGS=-Wall -Wextra -Iinclude
SRC=src/main.c src/game.c src/network.c
OUT=client.exe

ifeq ($(OS),Windows_NT)
	LIBS=-lws2_32
	RM=del /Q
else
	LIBS=
	OUT=client
	RM=rm -f
endif

all: client

client: $(SRC)
	$(CC) $(CFLAGS) $(SRC) $(LIBS) -o $(OUT)

clean:
	$(RM) $(OUT)
