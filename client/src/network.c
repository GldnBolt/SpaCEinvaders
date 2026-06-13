#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#if defined(_MSC_VER)
#pragma comment(lib, "ws2_32.lib")
#endif
#else
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include "network.h"

int init_sockets(void) {
#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2, 2), &wsaData) != 0) {
        fprintf(stderr, "ERROR: WSAStartup fallo.\n");
        return -1;
    }
#endif
    return 0;
}

int connect_to_server(const char *ip, int port) {
    int sockfd;
    struct sockaddr_in servaddr;

#ifdef _WIN32
    sockfd = (int)socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
#else
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
#endif

    if (sockfd < 0) {
        perror("ERROR: no se pudo crear el socket");
        exit(1);
    }

    memset(&servaddr, 0, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons((unsigned short)port);

#ifdef _WIN32
    if (InetPtonA(AF_INET, ip, &servaddr.sin_addr) != 1) {
        fprintf(stderr, "ERROR: direccion IP invalida: %s\n", ip);
        exit(1);
    }
#else
    if (inet_pton(AF_INET, ip, &servaddr.sin_addr) <= 0) {
        perror("ERROR: direccion IP invalida");
        exit(1);
    }
#endif

    if (connect(sockfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0) {
#ifdef _WIN32
        fprintf(stderr, "ERROR: no se pudo conectar al servidor. Codigo Winsock: %d\n", WSAGetLastError());
#else
        perror("ERROR: no se pudo conectar al servidor");
#endif
        exit(1);
    }

    return sockfd;
}

int send_message(int sockfd, const char *message) {
#ifdef _WIN32
    return send(sockfd, message, (int)strlen(message), 0);
#else
    return (int)send(sockfd, message, strlen(message), 0);
#endif
}

/*
 * Lee una linea completa terminada en \n.
 * Esto evita mezclar varios mensajes STATE en un solo recv.
 */
int receive_line(int sockfd, char *buffer, int size) {
    int total = 0;
    char ch;

    if (size <= 1) {
        return -1;
    }

    while (total < size - 1) {
#ifdef _WIN32
        int n = recv(sockfd, &ch, 1, 0);
#else
        int n = (int)recv(sockfd, &ch, 1, 0);
#endif
        if (n <= 0) {
            if (total == 0) {
                return n;
            }
            break;
        }

        if (ch == '\r') {
            continue;
        }

        if (ch == '\n') {
            break;
        }

        buffer[total++] = ch;
    }

    buffer[total] = '\0';
    return total;
}

void close_socket(int sockfd) {
#ifdef _WIN32
    closesocket((SOCKET)sockfd);
#else
    close(sockfd);
#endif
}

void cleanup_sockets(void) {
#ifdef _WIN32
    WSACleanup();
#endif
}
