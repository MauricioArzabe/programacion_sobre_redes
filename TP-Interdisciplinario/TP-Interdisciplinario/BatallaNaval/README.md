# Batalla Naval – Multiplayer (2 jugadores) por consola

Juego de Batalla Naval cliente-servidor en Java 17 con sockets TCP, hilos, flujos de objetos (`ObjectInputStream` / `ObjectOutputStream`) y POO.
Se usaron solo los temas de los repositorios de la materia:

| Tema del repo | Dónde se usa |
|---|---|
| `TPC_Socker_Thread` (ServerSocket, Socket, Runnable, Thread, AtomicBoolean, ConcurrentHashMap, synchronized, Colores ANSI, consola de admin) | `servidor/*`, `cliente/ClienteJuego`, `util/Colores` |
| `FileBinary` (Serializable, ObjectOutputStream / ObjectInputStream) | `protocolo/Mensaje`, `modelo/*` viajan serializados |
| `managerFile` (FileWriter, PrintWriter, FileReader, BufferedReader, split) | `servidor/Estadisticas` (historial en .txt) |
| `Flujos_de_Datos` (BufferedReader + InputStreamReader, String.format, StringBuilder, métodos de String) | lectura de teclado, tableros con formato |
| `Sistema-Club` (POO, paquetes, enum/clases con responsabilidades claras) | organización en paquetes `modelo`, `protocolo`, `servidor`, `cliente` |

## Estructura

```
src/main/java/ar/edu/et32/batallanaval/
├── modelo/
│   ├── TipoCelda.java      enum AGUA, BARCO, IMPACTO, FALLO
│   ├── Celda.java          tipo, disparada, nombreBarco
│   ├── Barco.java          nombre, longitud, impactos, recibirImpacto(), estaHundido()
│   ├── Flota.java          los 5 barcos que hay que colocar
│   └── Tablero.java        ArrayList<ArrayList<Celda>> 10x10, colocarBarco, disparar,
│                           mostrarTablero, mostrarBarcos, todosLosBarcosCaidos,
│                           verificarVictoria, verificarDerrota
├── protocolo/
│   ├── TipoMensaje.java    enum con todos los mensajes del protocolo
│   ├── Mensaje.java        objeto Serializable que viaja por el socket
│   └── Coordenada.java     "B7" <-> (fila 1, col 6)
├── servidor/
│   ├── MainServidor.java   punto de entrada
│   ├── Servidor.java       ServerSocket, accept(), un Thread por cliente, consola admin
│   ├── ManejadorCliente.java  Runnable que atiende a un cliente
│   ├── SalaEspera.java     empareja jugadores de a 2 (synchronized)
│   ├── Partida.java        estado de una partida, turnos, disparos (synchronized)
│   └── Estadisticas.java   historial en data/historial_partidas.txt
├── cliente/
│   ├── MainCliente.java    pide IP y puerto
│   └── ClienteJuego.java   hilo de teclado + hilo de recepción
└── util/Colores.java       colores ANSI
```

## Protocolo (objetos `Mensaje` serializados)

| Fase | Cliente → Servidor | Servidor → Cliente |
|---|---|---|
| Conexión / autenticación | `LOGIN(nick)` | `LOGIN_OK` o `ERROR` (nick inválido o en uso, puede reintentar) |
| Posición de barcos | `COLOCAR_BARCO(nombre, longitud, fila, col, horizontal)` x5 | `BARCO_OK` o `ERROR` (el servidor vuelve a validar) |
| | `LISTO` | `ESPERANDO_RIVAL` (sala de espera) |
| Inicio | | `INICIO_PARTIDA(nick rival)` |
| Turnos | | `TURNO` / `ESPERA_TURNO` |
| Disparo | `DISPARO(fila, col)` | al que tira: `RESULTADO_DISPARO(fila, col, "Agua"/"¡IMPACTO!"/"¡HUNDIDO!", barco)` · al rival: `DISPARO_RIVAL(...)` · inválido: `ERROR` + `TURNO` |
| Chat | `CHAT(texto)` | `CHAT(remitente, texto)` al rival |
| Estadísticas | `STATS` | `STATS(texto)` |
| Fin | `SALIR` (abandono) | `FIN_PARTIDA(texto, tablero del rival)` |

Los turnos se alternan. Si un jugador se desconecta, el otro gana por abandono.

## Hilos y sincronización

- Servidor: el hilo principal hace `accept()` y crea un `Thread` con un `ManejadorCliente` por conexión. Otro hilo (daemon) atiende la consola del administrador.
- `Partida.disparar()` / `abandono()` / `iniciar()` y `SalaEspera.ingresar()` son `synchronized`, así que dos jugadores no pueden modificar la misma partida a la vez.
- `ManejadorCliente.enviar()` es `synchronized` porque escriben en el mismo stream el hilo del jugador y el del rival.
- Jugadores y partidas se guardan en `ConcurrentHashMap`. El estado de conexión es un `AtomicBoolean`.
- Cliente: el hilo principal lee el teclado y el hilo `RECEPCION` lee los mensajes del servidor.

## Compilar y ejecutar (sin Docker)

Hace falta la JDK 17 o más nueva.

Windows:
```powershell
.\compilar.ps1
java -cp dist\BatallaNaval.jar ar.edu.et32.batallanaval.servidor.MainServidor 5000
java -cp dist\BatallaNaval.jar ar.edu.et32.batallanaval.cliente.MainCliente 127.0.0.1 5000
```
(Si los acentos se ven mal en la consola de Windows, ejecutá `chcp 65001` antes.)

Linux:
```sh
sh compilar.sh
sh servidor.sh 5000
sh cliente.sh 192.168.0.10 5000
```

En Eclipse: *File → Import → Maven → Existing Maven Projects* y seleccionar la carpeta (tiene `pom.xml`). Ejecutar `MainServidor` y después dos veces `MainCliente`.

### Comandos del cliente
| Comando | Acción |
|---|---|
| `B7` | disparar (letra A-J + número 1-10) |
| `/c hola` | chat con el rival |
| `/tableros` | volver a mostrar los tableros |
| `/barcos` | estado de tus barcos (ACTIVO/HUNDIDO) |
| `/stats` | estadísticas e historial |
| `/salir` | abandonar |

Para colocar barcos a mano se escribe la casilla inicial y la orientación: `A1 H` (horizontal) o `C4 V` (vertical). También se pueden colocar automáticamente.

### Comandos del servidor
`/jugadores`, `/partidas`, `/aviso texto`, `/salir`, `/ayuda`

## Docker (servidor)

```sh
docker compose up -d --build
docker compose logs -f          # ver lo que pasa en el servidor
docker attach batalla-naval-servidor   # usar la consola de admin (salir con Ctrl+P Ctrl+Q)
docker compose down
```
El servidor queda en el puerto `5000` de la máquina y el historial se guarda en `./data/historial_partidas.txt`.

## VM Debian liviana con los clientes

VM ya armada y probada: **`Debian-BatallaNaval`** en VirtualBox 7.2.

| | |
|---|---|
| Sistema | Debian 13.8 "Trixie" netinst, **sin entorno gráfico** (solo utilidades estándar + SSH) |
| Recursos | 1 CPU, 2 GB RAM, disco 10 GB (usa ~1,6 GB) |
| Red | NAT. Desde la VM, la PC (Docker) es `10.0.2.2`. SSH: puerto `2222` de la PC → `22` de la VM |
| Usuario | `alumno` (contraseña en `vm/credenciales_vm.txt`, también sirve para `sudo`) |
| Java | `default-jdk` → OpenJDK 21 (Debian 13 ya no trae `openjdk-17-jdk`; el código compila para Java 17 y corre igual) |

### Cómo se armó
1. Se descargó `debian-13.8.0-amd64-netinst.iso` de cdimage.debian.org.
2. Instalación desatendida de VirtualBox con la plantilla [`vm/debian_preseed_ligero.cfg`](vm/debian_preseed_ligero.cfg)
   (la plantilla original instalaba el escritorio GNOME; esta elige solo `standard, ssh-server`):
   ```powershell
   VBoxManage unattended install Debian-BatallaNaval --iso=debian-13.8.0-amd64-netinst.iso --user=alumno --user-password-file=vm\.pass --admin-password-file=vm\.pass --hostname=debian-bn.local --locale=es_AR --country=AR --time-zone=America/Argentina/Buenos_Aires --install-additions --package-selection-adjustment=minimal --script-template=vm\debian_preseed_ligero.cfg --start-vm=headless
   VBoxManage controlvm Debian-BatallaNaval natpf1 "ssh,tcp,127.0.0.1,2222,,22"
   ```
3. Dentro de la VM se instaló la JDK como pide la consigna:
   ```sh
   sudo apt update
   sudo apt install default-jdk
   java -version
   ```
4. Se copió el proyecto y se compiló en Linux:
   ```sh
   scp -P 2222 -r src pom.xml compilar.sh cliente.sh servidor.sh alumno@127.0.0.1:BatallaNaval/
   ssh -p 2222 alumno@127.0.0.1
   cd BatallaNaval && sh compilar.sh
   ```

### Jugar desde la VM
1. En la PC: `docker compose up -d` (servidor en el puerto 5000).
2. Encender la VM (`VBoxManage startvm Debian-BatallaNaval --type headless`, o desde la ventana de VirtualBox).
3. Abrir dos sesiones a la VM (dos `ssh -p 2222 alumno@127.0.0.1`, o `Alt+F1` / `Alt+F2` en la consola) y en cada una:
   ```sh
   cd BatallaNaval
   sh cliente.sh 10.0.2.2 5000
   ```

Si la VM está en modo **Adaptador puente** en vez de NAT, se usa la IP de la PC en la red (`ipconfig`) y hay que habilitar el puerto 5000 en el firewall de Windows.

### Nota sobre Windows Home + Docker
Con WSL 2 activo (lo necesita Docker Desktop), VirtualBox corre sobre el hipervisor de Windows ("NEM"): es más lento y con varias CPU la VM se colgó. Con **1 CPU** anda bien. Por eso los comandos se ejecutan por SSH.

Con Alpine es igual pero se instala con `apk add openjdk17`.
