# Documentación técnica del servidor Java - spaCEinvaders

## 1. Descripción general

El servidor Java es el componente central del proyecto spaCEinvaders. Su responsabilidad principal es mantener toda la lógica del juego, administrar el estado de la partida y comunicarse con los clientes mediante sockets TCP.

El servidor permite la conexión de clientes jugadores y clientes espectadores. Según la especificación, el servidor debe soportar como mínimo dos clientes jugadores. Los espectadores pueden conectarse a una partida existente, pero no pueden enviar acciones de juego.

## 2. Responsabilidades principales del servidor

El servidor Java se encarga de:

- Aceptar conexiones por sockets TCP.
- Registrar clientes jugadores y espectadores.
- Limitar la partida a un máximo de dos jugadores.
- Mantener el estado general del juego.
- Crear y destruir extraterrestres.
- Administrar el puntaje de cada jugador.
- Administrar las vidas de los jugadores.
- Crear OVNIs con dirección y puntaje.
- Controlar la velocidad de desplazamiento de los extraterrestres.
- Controlar el estado de los bunkers.
- Procesar acciones enviadas por los clientes.
- Enviar el estado actualizado del juego a todos los clientes.
- Impedir que los espectadores modifiquen la partida.

## 3. Estructura de paquetes

### `com.spaceinvaders`

Contiene la clase principal `MainServer`, que inicia el servidor.

### `com.spaceinvaders.server`

Contiene las clases relacionadas con la comunicación y administración del servidor:

- `GameServer`: servidor principal.
- `ClientHandler`: atiende a cada cliente conectado.
- `AdminConsole`: permite ingresar comandos administrativos.
- `ClientRole`: enum para distinguir jugadores y espectadores.
- `RegistrationResult`: representa el resultado de registrar un cliente.

### `com.spaceinvaders.model`

Contiene las entidades principales del juego:

- `GameState`: estado global del juego.
- `Player`: jugador.
- `Alien`: extraterrestre.
- `Bunker`: escudo terrestre.
- `UFO`: platillo volador.
- `Projectile`: disparo.

### `com.spaceinvaders.protocol`

Contiene clases para construir e interpretar mensajes:

- `Message`: representa un mensaje recibido.
- `MessageParser`: convierte texto recibido por sockets en objetos `Message`.
- `MessageBuilder`: construye mensajes de respuesta.
- `AdminCommandParser`: interpreta comandos administrativos.

### `com.spaceinvaders.game`

Contiene la lógica del ciclo del juego:

- `GameLoop`: actualiza periódicamente el estado del juego.

### `com.spaceinvaders.patterns.observer`

Implementa el patrón Observer:

- `GameObserver`
- `GameSubject`

### `com.spaceinvaders.patterns.factory`

Implementa el patrón Factory:

- `EntityFactory`

### `com.spaceinvaders.patterns.adapter`

Implementa el patrón Adapter:

- `NetworkCommand`
- `NetworkCommandType`
- `NetworkCommandAdapter`

### `com.spaceinvaders.tools`

Contiene herramientas temporales de prueba, como `ConsoleTestClient`. Este cliente no forma parte del cliente final, ya que el cliente final debe implementarse en C.

## 4. Patrones de diseño utilizados

### Observer

El patrón Observer se utiliza para notificar a todos los clientes conectados cada vez que cambia el estado del juego. El servidor actúa como sujeto observado y los clientes conectados actúan como observadores.

Esto permite que jugadores y espectadores reciban actualizaciones sin acoplar directamente la lógica del juego con cada socket individual.

### Factory

El patrón Factory se utiliza en `EntityFactory` para centralizar la creación de entidades del juego, como jugadores, extraterrestres, bunkers, disparos y OVNIs.

Esto evita distribuir llamadas directas a `new` por todo el código y mejora la organización del modelo.

### Adapter

El patrón Adapter se utiliza para convertir mensajes de red en comandos internos del servidor. Por ejemplo, un mensaje como:

`ACTION|playerId=1|cmd=MOVE_LEFT`

se convierte internamente en un comando `MOVE_LEFT`.

Esto separa el protocolo de comunicación de la lógica del juego.

## 5. Protocolo de comunicación

El protocolo usa texto plano por sockets TCP. Cada mensaje termina con salto de línea.

Formato general:

`TIPO|clave=valor|clave=valor`

Ejemplos de mensajes del cliente al servidor:

- `HELLO|role=PLAYER|name=Jugador1`
- `HELLO|role=SPECTATOR|name=Espectador1`
- `ACTION|playerId=1|cmd=MOVE_LEFT`
- `ACTION|playerId=1|cmd=MOVE_RIGHT`
- `ACTION|playerId=1|cmd=FIRE`
- `ALIEN_HIT|playerId=1|alienId=5`
- `DISCONNECT|playerId=1`

Ejemplos de mensajes del servidor al cliente:

- `WELCOME|role=PLAYER|playerId=1|maxPlayers=2`
- `STATE|tick=...|status=...|speed=...`
- `CREATE_ALIEN|id=...|x=...|y=...|points=...`
- `CREATE_UFO|direction=I-D|points=1500`
- `SET_SPEED|value=100`
- `SET_BUNKERS|health=70`
- `ERROR|code=...|message=...`

## 6. Comandos administrativos

El servidor permite ingresar comandos administrativos desde consola:

- `Crear (X,Y,Pts)`
- `OVNI I-D 1500`
- `OVNI D-I 1500`
- `Velocidad 100`
- `Bunkers 70%`

Estos comandos son interpretados por `AdminCommandParser` y aplicados al estado del juego.

## 7. Algoritmos principales

### Registro de clientes

Cuando un cliente se conecta, debe enviar un mensaje `HELLO`. El servidor analiza el rol del cliente. Si es jugador y hay menos de dos jugadores conectados, se le asigna un identificador. Si ya existen dos jugadores, el servidor rechaza la conexión como jugador. Si el cliente es espectador, se registra sin asignarle control de jugador.

### Actualización del juego

El `GameLoop` ejecuta actualizaciones periódicas. En cada ciclo se mueven los disparos, se mueven los extraterrestres, se generan disparos enemigos, se actualiza el OVNI y se revisan colisiones.

### Colisiones

El servidor revisa si los disparos intersectan con extraterrestres, bunkers, OVNI o jugadores. Cuando ocurre una colisión, se modifica el estado correspondiente: se elimina el disparo, se reduce la vida del bunker, se elimina el extraterrestre o se reduce la vida del jugador.

### Reinicio de ronda

Cuando todos los extraterrestres son eliminados, el servidor otorga una vida adicional a los jugadores, aumenta la velocidad y crea una nueva oleada de extraterrestres.

## 8. Ejecución

Para compilar:

`mvn clean package`

Para ejecutar el servidor:

`java -jar target/spaceinvaders-server-1.0-SNAPSHOT.jar`

Para ejecutar desde Maven:

`mvn exec:java`

## 9. Observaciones

El cliente `ConsoleTestClient` se utiliza únicamente como herramienta temporal para probar el servidor Java antes de integrar el cliente final en C. No reemplaza el cliente oficial solicitado por la especificación.