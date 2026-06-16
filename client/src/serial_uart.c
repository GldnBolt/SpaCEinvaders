#include "serial_uart.h"

#include <stdio.h>
#include <string.h>

#ifdef _WIN32

#define WIN32_LEAN_AND_MEAN
#include <windows.h>

static void print_windows_error(const char *context) {
    DWORD errorCode = GetLastError();
    char *message = NULL;

    FormatMessageA(
        FORMAT_MESSAGE_ALLOCATE_BUFFER |
        FORMAT_MESSAGE_FROM_SYSTEM |
        FORMAT_MESSAGE_IGNORE_INSERTS,
        NULL,
        errorCode,
        MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
        (LPSTR)&message,
        0,
        NULL
    );

    fprintf(stderr, "%s. Error Windows %lu", context, errorCode);

    if (message != NULL) {
        fprintf(stderr, ": %s", message);
        LocalFree(message);
    } else {
        fprintf(stderr, ".\n");
    }
}

SerialPort serial_open(const char *portName, int baudRate) {
    char fullPortName[64];
    HANDLE handle;
    DCB dcb;
    COMMTIMEOUTS timeouts;

    if (portName == NULL || portName[0] == '\0') {
        fprintf(stderr, "Puerto UART invalido.\n");
        return SERIAL_INVALID;
    }

    if (strncmp(portName, "\\\\.\\", 4) == 0) {
        snprintf(fullPortName, sizeof(fullPortName), "%s", portName);
    } else {
        snprintf(fullPortName, sizeof(fullPortName), "\\\\.\\%s", portName);
    }

    handle = CreateFileA(
        fullPortName,
        GENERIC_READ,
        0,
        NULL,
        OPEN_EXISTING,
        FILE_ATTRIBUTE_NORMAL,
        NULL
    );

    if (handle == INVALID_HANDLE_VALUE) {
        print_windows_error("No se pudo abrir el puerto serial");

        fprintf(stderr, "\nPosibles causas:\n");
        fprintf(stderr, "- El puerto no es el correcto.\n");
        fprintf(stderr, "- Arduino Serial Monitor esta abierto.\n");
        fprintf(stderr, "- Otro programa esta usando el puerto.\n");
        fprintf(stderr, "- La ESP32 no esta conectada.\n");

        return SERIAL_INVALID;
    }

    SetupComm(handle, 4096, 4096);

    memset(&dcb, 0, sizeof(dcb));
    dcb.DCBlength = sizeof(dcb);

    if (!GetCommState(handle, &dcb)) {
        print_windows_error("No se pudo leer la configuracion del puerto");
        CloseHandle(handle);
        return SERIAL_INVALID;
    }

    dcb.BaudRate = (DWORD)baudRate;
    dcb.ByteSize = 8;
    dcb.StopBits = ONESTOPBIT;
    dcb.Parity = NOPARITY;

    dcb.fBinary = TRUE;
    dcb.fParity = FALSE;

    dcb.fOutxCtsFlow = FALSE;
    dcb.fOutxDsrFlow = FALSE;
    dcb.fDtrControl = DTR_CONTROL_DISABLE;
    dcb.fRtsControl = RTS_CONTROL_DISABLE;
    dcb.fOutX = FALSE;
    dcb.fInX = FALSE;

    if (!SetCommState(handle, &dcb)) {
        print_windows_error("No se pudo configurar el puerto serial");
        CloseHandle(handle);
        return SERIAL_INVALID;
    }

    memset(&timeouts, 0, sizeof(timeouts));

    /*
       Lectura no bloqueante:
       si no hay datos disponibles, ReadFile retorna de inmediato.
    */
    timeouts.ReadIntervalTimeout = MAXDWORD;
    timeouts.ReadTotalTimeoutConstant = 0;
    timeouts.ReadTotalTimeoutMultiplier = 0;

    if (!SetCommTimeouts(handle, &timeouts)) {
        print_windows_error("No se pudieron configurar los timeouts del puerto");
        CloseHandle(handle);
        return SERIAL_INVALID;
    }

    PurgeComm(handle, PURGE_RXCLEAR | PURGE_TXCLEAR);

    printf("Puerto UART abierto correctamente: %s a %d baudios.\n", portName, baudRate);

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