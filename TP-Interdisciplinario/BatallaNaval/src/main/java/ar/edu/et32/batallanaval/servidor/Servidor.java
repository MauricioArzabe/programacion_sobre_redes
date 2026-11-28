package ar.edu.et32.batallanaval.servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import ar.edu.et32.batallanaval.protocolo.Mensaje;
import ar.edu.et32.batallanaval.protocolo.TipoMensaje;
import ar.edu.et32.batallanaval.util.Colores;

/**
 * Servidor de Batalla Naval. Acepta conexiones y crea un hilo por cliente.
 * Permite varias partidas simultáneas.
 */
public class Servidor implements AutoCloseable {

    private final ServerSocket server;
    private volatile boolean ejecutando = true;

    private final ConcurrentMap<String, ManejadorCliente> jugadores = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Partida> partidas = new ConcurrentHashMap<>();
    private final AtomicInteger contadorPartidas = new AtomicInteger(0);

    private final SalaEspera sala = new SalaEspera(this);
    private final Estadisticas estadisticas;

    public Servidor(int puerto, String rutaHistorial) throws IOException {
        server = new ServerSocket(puerto);
        estadisticas = new Estadisticas(rutaHistorial);
    }

    public void iniciar() {
        System.out.println(Colores.VERDE + "Servidor de Batalla Naval escuchando en puerto "
                + server.getLocalPort() + Colores.RESET);
        System.out.println(Colores.GRIS + "Escribí /ayuda para ver los comandos del administrador." + Colores.RESET);

        Thread consola = new Thread(this::consolaAdministracion, "CONSOLA-SERVIDOR");
        consola.setDaemon(true);
        consola.start();

        while (ejecutando) {
            try {
                Socket socket = server.accept();
                System.out.println(Colores.AZUL + "[CONEXIÓN] " + socket.getInetAddress().getHostAddress()
                        + ":" + socket.getPort() + Colores.RESET);

                // Cada cliente en un hilo separado
                new Thread(new ManejadorCliente(socket, this), "CLIENTE-" + socket.getPort()).start();

            } catch (IOException e) {
                if (ejecutando) {
                    System.err.println(Colores.ROJO + "[ERROR] " + e.getMessage() + Colores.RESET);
                }
                break;
            }
        }
    }

    /** Devuelve null si se registró bien, o el texto del error. */
    public String registrar(String nick, ManejadorCliente cliente) {
        if (nick == null || nick.isBlank() || nick.length() > 15
                || nick.contains(";") || nick.startsWith("/")
                || nick.chars().anyMatch(Character::isISOControl)) {
            return "Nickname inválido (1 a 15 caracteres, sin ';' ni '/').";
        }
        if (jugadores.putIfAbsent(nick.toLowerCase(Locale.ROOT), cliente) != null) {
            return "Ese nickname ya está en uso.";
        }
        return null;
    }

    public void crearPartida(ManejadorCliente j1, ManejadorCliente j2) {
        Partida p = new Partida(contadorPartidas.incrementAndGet(), j1, j2, this);
        partidas.put(p.getId(), p);
        p.iniciar();
    }

    public void quitarPartida(Partida p) {
        partidas.remove(p.getId());
    }

    public void desconectar(ManejadorCliente cliente) {
        sala.quitar(cliente);

        Partida p = cliente.getPartida();
        if (p != null) {
            p.abandono(cliente);
        }

        if (cliente.getNickName() != null
                && jugadores.remove(cliente.getNickName().toLowerCase(Locale.ROOT), cliente)) {
            System.out.println(Colores.AMARILLO + "[DESCONECTADO] " + cliente.getNickName() + Colores.RESET);
        }
    }

    private void consolaAdministracion() {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));
        try {
            String linea;
            while (ejecutando && (linea = teclado.readLine()) != null) {
                linea = linea.trim();

                if (linea.equalsIgnoreCase("/ayuda")) {
                    System.out.println(Colores.AMARILLO
                            + "/jugadores        - lista los jugadores conectados\n"
                            + "/partidas         - lista las partidas en curso\n"
                            + "/aviso texto      - mensaje a todos los jugadores\n"
                            + "/salir            - detiene el servidor" + Colores.RESET);

                } else if (linea.equalsIgnoreCase("/jugadores")) {
                    System.out.println(Colores.CYAN + "[JUGADORES] " + jugadores.size() + " conectados, "
                            + sala.cantidad() + " en sala de espera" + Colores.RESET);
                    for (ManejadorCliente c : jugadores.values()) {
                        System.out.println("  - " + c.getNickName()
                                + (c.getPartida() != null ? " (jugando #" + c.getPartida().getId() + ")" : ""));
                    }

                } else if (linea.equalsIgnoreCase("/partidas")) {
                    System.out.println(Colores.CYAN + "[PARTIDAS EN CURSO] " + partidas.size() + Colores.RESET);
                    for (Partida p : partidas.values()) {
                        System.out.println("  " + p);
                    }

                } else if (linea.regionMatches(true, 0, "/aviso ", 0, 7)) {
                    String texto = linea.substring(7).trim();
                    for (ManejadorCliente c : jugadores.values()) {
                        c.enviar(new Mensaje(TipoMensaje.INFO, "[SERVIDOR] " + texto));
                    }

                } else if (linea.equalsIgnoreCase("/salir")) {
                    close();
                    break;

                } else if (!linea.isEmpty()) {
                    System.out.println("Comando desconocido. Escribí /ayuda.");
                }
            }
        } catch (IOException e) {
            if (ejecutando) {
                System.err.println("Error leyendo la consola del servidor.");
            }
        }
    }

    public SalaEspera getSala() {
        return sala;
    }

    public Estadisticas getEstadisticas() {
        return estadisticas;
    }

    @Override
    public void close() {
        if (!ejecutando) {
            return;
        }
        ejecutando = false;
        try {
            if (!server.isClosed()) {
                server.close();
            }
        } catch (IOException e) {
            System.err.println("Error cerrando el servidor.");
        }

        for (ManejadorCliente c : jugadores.values()) {
            c.enviar(new Mensaje(TipoMensaje.INFO, "El servidor se está cerrando."));
            c.cerrar();
        }
        jugadores.clear();
        System.out.println(Colores.ROJO + "Servidor detenido." + Colores.RESET);
    }
}
