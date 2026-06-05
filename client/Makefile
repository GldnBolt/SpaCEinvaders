CC=gcc
CFLAGS=-Wall -Iinclude

all: client

client: src/main.c
	$(CC) $(CFLAGS) src/main.c -o client

clean:
	rm -f client