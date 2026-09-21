#ifndef NETWORK_H
#define NETWORK_H

int init_sockets(void);
int connect_to_server(const char *ip, int port);
int send_message(int sockfd, const char *message);
int receive_line(int sockfd, char *buffer, int size);
void close_socket(int sockfd);
void cleanup_sockets(void);

#endif
