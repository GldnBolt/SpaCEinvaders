#ifndef SERIAL_UART_H
#define SERIAL_UART_H

#include <stdint.h>

typedef intptr_t SerialPort;

#define SERIAL_INVALID ((SerialPort)-1)

SerialPort serial_open(const char *portName, int baudRate);
int serial_read_command(SerialPort port, char *out, int outSize);
void serial_close(SerialPort port);

#endif