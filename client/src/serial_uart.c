#include "serial_uart.h"

#include <stdio.h>
#include <string.h>

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

SerialPort serial_open(const char *portName, int baudRate) {
    char fullPortName[64];
    HANDLE handle;
    DCB dcb;
    COMMTIMEOUTS timeouts;

    if (strncmp(portName, "\\\\.\\", 4) == 0) {
        snprintf(fullPortName, sizeof(fullPortName), "%s", portName);
    } else {
        snprintf(fullPortName, sizeof(fullPortName), "\\\\.\\%s", portName);
    }

    handle = CreateFileA(
        fullPortName,
        GENERIC_READ | GENERIC_WRITE,
        0,
        NULL,
        OPEN_EXISTING,
        0,
        NULL
    );

    if (handle == INVALID_HANDLE_VALUE) {
        return SERIAL_INVALID;
    }

    memset(&dcb, 0, sizeof(dcb));
    dcb.DCBlength = sizeof(dcb);

    if (!GetCommState(handle, &dcb)) {
        CloseHandle(handle);
        return SERIAL_INVALID;
    }

    dcb.BaudRate = (DWORD)baudRate;
    dcb.ByteSize = 8;
    dcb.StopBits = ONESTOPBIT;
    dcb.Parity = NOPARITY;
    dcb.fDtrControl = DTR_CONTROL_ENABLE;
    dcb.fRtsControl = RTS_CONTROL_ENABLE;

    if (!SetCommState(handle, &dcb)) {
        CloseHandle(handle);
        return SERIAL_INVALID;
    }

    memset(&timeouts, 0, sizeof(timeouts));
    timeouts.ReadIntervalTimeout = 1;
    timeouts.ReadTotalTimeoutConstant = 1;
    timeouts.ReadTotalTimeoutMultiplier = 1;

    SetCommTimeouts(handle, &timeouts);
    PurgeComm(handle, PURGE_RXCLEAR | PURGE_TXCLEAR);

    return (SerialPort)(intptr_t)handle;
}

int serial_read_command(SerialPort port, char *out, int outSize) {
    static char line[128];
    static int length = 0;

    HANDLE handle;
    DWORD bytesRead = 0;
    char ch;

    if (port == SERIAL_INVALID || out == NULL || outSize <= 0) {
        return 0;
    }

    handle = (HANDLE)(intptr_t)port;

    while (ReadFile(handle, &ch, 1, &bytesRead, NULL) && bytesRead == 1) {
        if (ch == '\r') {
            continue;
        }

        if (ch == '\n') {
            if (length == 0) {
                continue;
            }

            line[length] = '\0';
            snprintf(out, (size_t)outSize, "%s", line);
            length = 0;
            return 1;
        }

        if (length < (int)sizeof(line) - 1) {
            line[length++] = ch;
        } else {
            length = 0;
        }
    }

    return 0;
}

void serial_close(SerialPort port) {
    if (port != SERIAL_INVALID) {
        CloseHandle((HANDLE)(intptr_t)port);
    }
}

#else

SerialPort serial_open(const char *portName, int baudRate) {
    (void)portName;
    (void)baudRate;
    return SERIAL_INVALID;
}

int serial_read_command(SerialPort port, char *out, int outSize) {
    (void)port;
    (void)out;
    (void)outSize;
    return 0;
}

void serial_close(SerialPort port) {
    (void)port;
}

#endif