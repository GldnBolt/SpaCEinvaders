#ifdef _WIN32
#include <winsock2.h>
#include <ws2tcpip.h>
#pragma comment(lib, "ws2_32.lib")
#else
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#endif

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

int init_sockets() {
#ifdef _WIN32
    WSADATA wsaData;
    if (WSAStartup(MAKEWORD(2,2), &wsaData) != 0) {
        fprintf(stderr, "WSAStartup failed\n");
        return -1;
    }
#endif
    return 0;
}

int connect_to_server(const char *ip, int port) {
    int sockfd;
    struct sockaddr_in servaddr;

#ifdef _WIN32
    sockfd = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
#else
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
#endif
    if(sockfd < 0) {
        perror("socket failed");
        exit(1);
    }

    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(port);

#ifdef _WIN32
    InetPton(AF_INET, ip, &servaddr.sin_addr);
#else
    if(inet_pton(AF_INET, ip, &servaddr.sin_addr) <= 0) {
        perror("Invalid address");
        exit(1);
    }
#endif

    if(connect(sockfd, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0) {
#ifdef _WIN32
        fprintf(stderr, "connect failed: %d\n", WSAGetLastError());
#else
        perror("connect failed");
#endif
        exit(1);
    }

    return sockfd;
}

int send_message(int sockfd, const char *message) {
#ifdef _WIN32
    return send(sockfd, message, (int)strlen(message), 0);
#else
    return send(sockfd, message, strlen(message), 0);
#endif
}

int receive_message(int sockfd, char *buffer, int size) {
#ifdef _WIN32
    int n = recv(sockfd, buffer, size-1, 0);
#else
    int n = recv(sockfd, buffer, size-1, 0);
#endif
    if(n >= 0) buffer[n] = '\0';
    return n;
}

void close_socket(int sockfd) {
#ifdef _WIN32
    closesocket(sockfd);
#else
    close(sockfd);
#endif
}

void cleanup_sockets() {
#ifdef _WIN32
    WSACleanup();
#endif
}