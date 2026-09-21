# spaCEinvaders

Proyecto desarrollado para el curso **Paradigmas de Programación (CE1106)** del Instituto Tecnológico de Costa Rica.

## Integrantes

* Andrés Blanco Coto
* Gabriel Bolaños Barboza
* Fabiana Moreno Castrillo

## Descripción general

**spaCEinvaders** es una aplicación cliente-servidor inspirada en el videojuego clásico *Space Invaders*. El proyecto integra dos paradigmas de programación:

* **Programación orientada a objetos en Java**, utilizada para desarrollar el servidor.
* **Programación imperativa en C**, utilizada para desarrollar el cliente gráfico y el cliente de prueba.

El servidor Java mantiene la lógica principal del juego, mientras que los clientes en C se encargan de mostrar la interfaz gráfica, recibir entradas del jugador y comunicarse con el servidor mediante sockets TCP.

## Arquitectura del proyecto

El sistema utiliza una arquitectura cliente-servidor:

```text
Servidor Java
├── Mantiene el estado oficial del juego
├── Administra jugadores, vidas, puntos, aliens, bunkers, OVNI y velocidad
├── Procesa acciones recibidas por sockets
├── Notifica el estado actualizado a los clientes
└── Permite clientes jugadores y espectadores

Cliente C
├── Se conecta al servidor mediante sockets
├── Recibe mensajes STATE
├── Dibuja el juego usando raylib
├── Permite mover el cañón y disparar
├── Soporta modo jugador y modo espectador
└── Puede recibir comandos de un control físico por UART
```

## Funcionalidades implementadas

* Servidor en Java.
* Cliente gráfico en C.
* Comunicación cliente-servidor mediante sockets TCP.
* Soporte para dos clientes jugadores.
* Soporte para cliente espectador.
* Movimiento de jugadores.
* Disparo de jugadores.
* Gestión de vidas.
* Gestión de puntajes.
* Movimiento de extraterrestres.
* Gestión de proyectiles.
* Gestión de bunkers.
* Gestión de OVNI.
* Comandos administrativos desde consola del servidor.
* Reinicio de ronda.
* Aumento de velocidad al completar una ronda.
* Uso de patrones de diseño en Java.
* Uso de `structs` y constantes separadas en C.
* Soporte para control físico ESP32 mediante UART.

## Estructura del proyecto

```text
SpaCEinvaders/
├── client/
│   ├── include/
│   │   ├── constants.h
│   │   ├── game_structs.h
│   │   └── network.h
│   ├── src/
│   │   ├── main.c
│   │   └── network.c
│   └── graphics/
│       ├── spaceinvaders_graphic.c
│       └── spaceinvaders_graphic.exe
│
├── src/main/java/com/spaceinvaders/
│   ├── MainServer.java
│   ├── server/
│   ├── model/
│   ├── protocol/
│   ├── game/
│   ├── patterns/
│   └── tools/
│
├── scripts/
│   ├── build-client.bat
│   ├── build-graphic-client.bat
│   ├── run-server.bat
│   ├── run-player1-test.bat
│   ├── run-player2-test.bat
│   └── run-spectator-test.bat
│
├── dist/
│   ├── spaceinvaders-server-1.0-SNAPSHOT.jar
│   ├── client.exe
│   └── spaceinvaders_graphic.exe
│
├── docs/
├── pom.xml
└── README.md
```

## Requisitos

### Para el servidor Java

* Java JDK 17 o superior.
* Apache Maven.
* Puerto 5000 disponible.

### Para el cliente C

* GCC o MinGW.
* En Windows, biblioteca Winsock `ws2_32`.
* Para el cliente gráfico, raylib instalada correctamente.
* En caso de usar el control físico, ESP32 conectada por puerto serial.

## Compilar el servidor Java

Desde la raíz del proyecto, ejecutar:

```bash
mvn clean package
```

Esto genera el archivo `.jar` en:

```text
target/spaceinvaders-server-1.0-SNAPSHOT.jar
```

También se recomienda copiarlo a la carpeta `dist/`:

```bash
copy target\spaceinvaders-server-1.0-SNAPSHOT.jar dist\
```

## Ejecutar el servidor Java

Desde la raíz del proyecto:

```bash
java -jar dist/spaceinvaders-server-1.0-SNAPSHOT.jar
```

También puede ejecutarse con Maven:

```bash
mvn exec:java
```

Al iniciar correctamente, se mostrará:

```text
Servidor spaCEinvaders iniciado en puerto 5000
Esperando clientes...
admin>
```

## Comandos administrativos del servidor

Desde la consola del servidor se pueden usar los siguientes comandos:

```text
Crear (X,Y,Pts)
OVNI I-D 1500
OVNI D-I 1500
Velocidad 100
Bunkers 70%
ayuda
salir
```

Ejemplos:

```text
Crear (100,80,10)
OVNI I-D 1500
Velocidad 180
Bunkers 40%
```

Descripción:

* `Crear (X,Y,Pts)`: crea un extraterrestre en la posición indicada con el puntaje indicado.
* `OVNI I-D 1500`: crea un OVNI que se mueve de izquierda a derecha.
* `OVNI D-I 1500`: crea un OVNI que se mueve de derecha a izquierda.
* `Velocidad 100`: modifica la velocidad del juego.
* `Bunkers 70%`: cambia la resistencia de los bunkers.
* `ayuda`: muestra los comandos disponibles.
* `salir`: cierra el servidor correctamente.

## Compilar el cliente C de consola

Desde la raíz del proyecto:

```bash
scripts\build-client.bat
```

También puede compilarse manualmente con:

```bash
gcc -Iclient/include client/src/main.c client/src/network.c -o client/client.exe -lws2_32
```

Luego copiar a `dist/`:

```bash
copy client\client.exe dist\
```

## Ejecutar el cliente C de consola

Primero debe estar activo el servidor Java.

Luego ejecutar:

```bash
dist\client.exe
```

Este cliente se usa principalmente para pruebas de comunicación por sockets.

## Compilar el cliente gráfico en C

Desde la raíz del proyecto:

```bash
scripts\build-graphic-client.bat
```

Si la compilación fue exitosa, se genera:

```text
client/graphics/spaceinvaders_graphic.exe
```

Luego copiar a `dist/`:

```bash
copy client\graphics\spaceinvaders_graphic.exe dist\
```

## Ejecutar el cliente gráfico en C

Primero iniciar el servidor Java:

```bash
java -jar dist/spaceinvaders-server-1.0-SNAPSHOT.jar
```

Luego ejecutar el cliente gráfico:

```bash
dist\spaceinvaders_graphic.exe
```

El cliente se conectará al servidor, recibirá el estado del juego y mostrará la partida gráficamente.

## Controles del cliente gráfico

```text
A       Mover cañón a la izquierda
D       Mover cañón a la derecha
F       Disparar
Espacio Disparar
Q       Salir
ESC     Salir
```

## Cliente espectador

El proyecto permite ejecutar un cliente en modo espectador. El espectador recibe el estado del juego y observa la partida, pero no puede mover ni disparar.

El modo espectador depende de la configuración utilizada en el cliente gráfico o en los scripts de prueba.

## Control físico ESP32

El proyecto incluye soporte para un control físico basado en ESP32, autorizado para funcionar como dispositivo de entrada.

La ESP32 se comunica con el cliente C mediante UART. El cliente interpreta los comandos recibidos por puerto serial y los transforma en acciones del juego.

Comandos esperados:

```text
L   Mover a la izquierda
R   Mover a la derecha
F   Disparar
```

La ESP32 no se comunica directamente con el servidor Java. Funciona como dispositivo de entrada del cliente C.

## Protocolo de comunicación

La comunicación se realiza mediante texto plano sobre sockets TCP. Cada mensaje termina con salto de línea.

Formato general:

```text
TIPO|clave=valor|clave=valor
```

### Mensajes principales del cliente al servidor

```text
HELLO|role=PLAYER|name=Jugador1
HELLO|role=SPECTATOR|name=Espectador1
ACTION|playerId=1|cmd=MOVE_LEFT
ACTION|playerId=1|cmd=MOVE_RIGHT
ACTION|playerId=1|cmd=FIRE
ALIEN_HIT|playerId=1|alienId=5
DISCONNECT|playerId=1
```

### Mensajes principales del servidor al cliente

```text
WELCOME|role=PLAYER|playerId=1|maxPlayers=2
WELCOME|role=SPECTATOR|playerId=0|maxPlayers=2
STATE|tick=...|status=...|speed=...|players=...|aliens=...|bunkers=...|ufo=...|playerShots=...|enemyShots=...
CREATE_ALIEN|id=...|x=...|y=...|points=...
CREATE_UFO|direction=I-D|points=1500
SET_SPEED|value=100
SET_BUNKERS|health=70
ERROR|code=...|message=...
```

## Patrones de diseño utilizados en Java

### Observer

El servidor utiliza Observer para notificar a todos los clientes conectados cuando cambia el estado del juego. Cada cliente funciona como observador y recibe los mensajes actualizados del servidor.

### Factory

El patrón Factory se utiliza para centralizar la creación de entidades del juego, como jugadores, extraterrestres, bunkers, proyectiles y OVNI.

### Adapter

El patrón Adapter se utiliza para convertir mensajes recibidos desde la red en comandos internos del servidor. Esto separa el protocolo de comunicación de la lógica principal del juego.

## Ejecutables recomendados para entrega

La carpeta `dist/` debe contener:

```text
dist/spaceinvaders-server-1.0-SNAPSHOT.jar
dist/spaceinvaders_graphic.exe
dist/client.exe
```

El archivo más importante del servidor es:

```text
spaceinvaders-server-1.0-SNAPSHOT.jar
```

El archivo más importante del cliente es:

```text
spaceinvaders_graphic.exe
```

El archivo `client.exe` se incluye como respaldo para pruebas de comunicación por consola.

## Orden recomendado de ejecución

1. Ejecutar el servidor Java:

```bash
java -jar dist/spaceinvaders-server-1.0-SNAPSHOT.jar
```

2. Ejecutar el primer cliente gráfico:

```bash
dist\spaceinvaders_graphic.exe
```

3. Ejecutar el segundo cliente gráfico:

```bash
dist\spaceinvaders_graphic.exe
```

4. Opcionalmente, ejecutar un cliente espectador.

5. Opcionalmente, conectar el control físico ESP32 por UART.

## Solución de problemas comunes

### El servidor no inicia porque el puerto está ocupado

Cerrar cualquier servidor anterior o revisar el proceso que usa el puerto 5000.

En Windows:

```bash
netstat -ano | findstr :5000
taskkill /PID <PID> /F
```

### El cliente no se conecta

Verificar que el servidor esté activo antes de abrir el cliente.

También revisar que el cliente esté usando:

```text
IP: 127.0.0.1
Puerto: 5000
```

### El cliente gráfico no compila

Verificar que raylib esté instalada y que el compilador usado sea el correcto. Se recomienda usar MSYS2 UCRT64 o el entorno donde raylib esté configurado.

### Error relacionado con Winsock

En Windows, el cliente C debe compilarse enlazando la biblioteca `ws2_32`:

```bash
-lws2_32
```

### El control físico no responde

Verificar:

* Puerto COM correcto.
* ESP32 conectada.
* Ningún monitor serial usando el puerto.
* Velocidad UART configurada correctamente.
* Comandos enviados como `L`, `R` o `F`.

## Notas finales

El servidor Java mantiene la lógica oficial del juego. Los clientes C no calculan el resultado principal de la partida, sino que envían acciones y representan gráficamente el estado recibido del servidor.

Esta separación permite mantener sincronizados a los jugadores y espectadores conectados, evitando inconsistencias entre clientes.
