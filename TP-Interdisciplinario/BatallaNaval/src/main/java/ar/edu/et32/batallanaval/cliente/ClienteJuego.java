package ar.edu.et32.batallanaval.cliente;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.edu.et32.batallanaval.modelo.Flota;
import ar.edu.et32.batallanaval.modelo.Tablero;
import ar.edu.et32.batallanaval.protocolo.Coordenada;
import ar.edu.et32.batallanaval.protocolo.Mensaje;
import ar.edu.et32.batallanaval.protocolo.TipoMensaje;
import ar.edu.et32.batallanaval.util.Colores;

/**
 * Lógica del cliente:
 *  - hilo principal: lee el teclado (barcos, disparos, comandos)
 *  - hilo RECEPCION: lee los mensajes del servidor y actualiza la pantalla
 */
public class ClienteJuego {

    private final String host;
    private final int puerto;
    private final BufferedReader teclado;

    private ObjectOutputStream salida;
    private ObjectInputStream entrada;

    private String nickName;
    private String rival = "rival";
    private final Tablero propio = new Tablero();
    private final Tablero enemigo = new Tablero();

    private final AtomicBoolean miTurno = new AtomicBoolean(false);
    private final AtomicBoolean enPartida = new AtomicBoolean(false);
    private final AtomicBoolean terminado = new AtomicBoolean(false);

    public ClienteJuego(String host, int puerto, BufferedReader teclado) {
        this.host = host;
        this.puerto = puerto;
        this.teclado = teclado;
    }

    public void jugar() {
        try (Socket socket = new Socket(host, puerto)) {
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            System.out.println(Colores.VERDE + "Conectado a " + host + ":" + puerto + Colores.RESET);

            if (!login() || !colocarFlota()) {
                return;
            }
            enviar(new Mensaje(TipoMensaje.LISTO));

            Thread recepcion = new Thread(this::recibirMensajes, "RECEPCION");
            recepcion.start();

            bucleTeclado();

            if (!terminado.get()) {
                try {
                    enviar(new Mensaje(TipoMensaje.SALIR));
                } catch (IOException ignored) {
                    // el servidor ya cerró
                }
            }
            terminado.set(true);
            socket.close(); // libera al hilo bloqueado en readObject()
            recepcion.join();
            System.out.println(Colores.AMARILLO + "Desconectado. ¡Gracias por jugar!" + Colores.RESET);

        } catch (SocketException e) {
            System.err.println(Colores.ROJO + "Problema de conexión: " + e.getMessage() + Colores.RESET);
        } catch (IOException e) {
            System.err.println(Colores.ROJO + "Error de E/S: " + e.getMessage() + Colores.RESET);
        } catch (ClassNotFoundException e) {
            System.err.println(Colores.ROJO + "Respuesta desconocida del servidor." + Colores.RESET);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===================================================================== LOGIN

    private boolean login() throws IOException, ClassNotFoundException {
        while (true) {
            System.out.print(Colores.AMARILLO + "Ingresá tu nickname: " + Colores.RESET);
            String nick = teclado.readLine();
            if (nick == null) {
                return false;
            }
            enviar(Mensaje.login(nick.trim()));

            Mensaje resp = recibir();
            if (resp.getTipo() == TipoMensaje.LOGIN_OK) {
                nickName = nick.trim();
                System.out.println(Colores.VERDE + resp.getTexto() + Colores.RESET);
                return true;
            }
            System.out.println(Colores.ROJO + "[ERROR] " + resp.getTexto() + Colores.RESET);
        }
    }

    // ======================================================== COLOCAR LOS BARCOS

    private boolean colocarFlota() throws IOException, ClassNotFoundException {
        System.out.println(Colores.CYAN + "\n--- POSICIONAMIENTO DE LA FLOTA ---" + Colores.RESET);
        System.out.print("¿Colocar los barcos automáticamente? (s/n): ");
        String r = teclado.readLine();
        if (r == null) {
            return false;
        }
        boolean automatico = r.trim().equalsIgnoreCase("s");
        Random random = new Random();

        for (int i = 0; i < Flota.cantidad(); i++) {
            String nombre = Flota.NOMBRES[i];
            int largo = Flota.LONGITUDES[i];
            int fila;
            int col;
            boolean horizontal;

            while (true) {
                if (automatico) {
                    fila = random.nextInt(Tablero.TAMANIO);
                    col = random.nextInt(Tablero.TAMANIO);
                    horizontal = random.nextBoolean();
                } else {
                    propio.mostrarTablero(false);
                    System.out.printf("%s%s (%d casillas)%s - inicio y orientación (ej: A1 H / C4 V): ",
                            Colores.AMARILLO, nombre, largo, Colores.RESET);
                    String linea = teclado.readLine();
                    if (linea == null) {
                        return false;
                    }
                    String[] partes = linea.trim().split("\\s+");
                    int[] pos = partes.length == 2 ? Coordenada.parsear(partes[0]) : null;
                    if (pos == null || !(partes[1].equalsIgnoreCase("H") || partes[1].equalsIgnoreCase("V"))) {
                        System.out.println(Colores.ROJO + "Formato inválido. Ejemplo: B3 H" + Colores.RESET);
                        continue;
                    }
                    fila = pos[0];
                    col = pos[1];
                    horizontal = partes[1].equalsIgnoreCase("H");
                }

                // Validación local (no se sale del tablero ni se superpone)
                if (propio.colocarBarco(nombre, fila, col, largo, horizontal)) {
                    break;
                }
                if (!automatico) {
                    System.out.println(Colores.ROJO + "No entra ahí o se superpone con otro barco." + Colores.RESET);
                }
            }

            // El servidor vuelve a validar
            enviar(Mensaje.colocarBarco(nombre, largo, fila, col, horizontal));
            Mensaje resp = recibir();
            if (resp.getTipo() != TipoMensaje.BARCO_OK) {
                System.out.println(Colores.ROJO + "[ERROR] " + resp.getTexto() + Colores.RESET);
                return false;
            }
        }

        System.out.println(Colores.VERDE + "\nFlota lista:" + Colores.RESET);
        propio.mostrarTablero(false);
        propio.mostrarBarcos();
        return true;
    }

    // ==================================================== TECLADO (hilo principal)

    private void bucleTeclado() throws IOException {
        String linea;
        while (!terminado.get() && (linea = teclado.readLine()) != null) {
            if (terminado.get()) {
                break;
            }
            linea = linea.trim();
            if (linea.isEmpty()) {
                continue;
            }

            if (linea.startsWith("/")) {
                if (!comando(linea)) {
                    break;
                }
                continue;
            }

            if (!enPartida.get()) {
                System.out.println(Colores.GRIS + "Todavía no empezó la partida. Esperá a tu rival." + Colores.RESET);
                continue;
            }
            if (!miTurno.get()) {
                System.out.println(Colores.GRIS + "No es tu turno. (Podés chatear con /c mensaje)" + Colores.RESET);
                continue;
            }

            int[] pos = Coordenada.parsear(linea);
            if (pos == null) {
                System.out.println(Colores.ROJO + "Coordenada inválida. Usá letra A-J y número 1-10 (ej: B7)."
                        + Colores.RESET);
                continue;
            }
            if (enemigo.getCelda(pos[0], pos[1]).isDisparada()) {
                System.out.println(Colores.ROJO + "Ya disparaste en " + Coordenada.aTexto(pos[0], pos[1]) + "."
                        + Colores.RESET);
                continue;
            }

            miTurno.set(false);
            enviar(Mensaje.disparo(pos[0], pos[1]));
        }
    }

    /** Devuelve false si el jugador quiere salir. */
    private boolean comando(String linea) throws IOException {
        String[] partes = linea.split("\\s+", 2);
        String cmd = partes[0].toLowerCase();

        switch (cmd) {
            case "/ayuda":
                mostrarAyuda();
                break;
            case "/tableros":
                dibujarTableros(enemigo, true);
                break;
            case "/barcos":
                propio.mostrarBarcos();
                break;
            case "/c":
            case "/chat":
                if (partes.length < 2 || partes[1].isBlank()) {
                    System.out.println(Colores.ROJO + "Usá /c mensaje" + Colores.RESET);
                } else {
                    enviar(new Mensaje(TipoMensaje.CHAT, partes[1]));
                }
                break;
            case "/stats":
                enviar(new Mensaje(TipoMensaje.STATS));
                break;
            case "/salir":
                return false;
            default:
                System.out.println(Colores.ROJO + "Comando desconocido. Escribí /ayuda." + Colores.RESET);
        }
        return true;
    }

    // ============================================== RECEPCIÓN (hilo secundario)

    private void recibirMensajes() {
        try {
            while (!terminado.get()) {
                procesar(recibir());
            }
        } catch (EOFException | SocketException e) {
            if (!terminado.get()) {
                System.out.println(Colores.ROJO + "\nConexión cerrada por el servidor. Presioná ENTER."
                        + Colores.RESET);
                terminado.set(true);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!terminado.get()) {
                System.out.println(Colores.ROJO + "\nError de comunicación: " + e.getMessage() + Colores.RESET);
                terminado.set(true);
            }
        }
    }

    private void procesar(Mensaje m) {
        switch (m.getTipo()) {
            case ESPERANDO_RIVAL:
                System.out.println(Colores.AMARILLO + "[SALA] " + m.getTexto() + Colores.RESET);
                break;

            case INICIO_PARTIDA:
                rival = m.getTexto();
                enPartida.set(true);
                System.out.println(Colores.VERDE + Colores.NEGRITA + "\n¡Partida encontrada! Jugás contra "
                        + rival + Colores.RESET);
                System.out.println(Colores.GRIS + "Escribí /ayuda para ver los comandos." + Colores.RESET);
                break;

            case TURNO:
                miTurno.set(true);
                dibujarTableros(enemigo, true);
                System.out.print(Colores.VERDE + Colores.NEGRITA + ">> TU TURNO. Disparo (ej: B7): " + Colores.RESET);
                break;

            case ESPERA_TURNO:
                miTurno.set(false);
                dibujarTableros(enemigo, true);
                System.out.println(Colores.GRIS + ">> Turno de " + rival + ". Esperando su disparo..." + Colores.RESET);
                break;

            case RESULTADO_DISPARO:
                enemigo.marcarDisparo(m.getFila(), m.getCol(), m.isImpacto());
                System.out.println(textoResultado("Disparaste a", m));
                break;

            case DISPARO_RIVAL:
                propio.disparar(m.getFila(), m.getCol()); // actualiza el tablero propio
                System.out.println(textoResultado(rival + " disparó a", m));
                break;

            case CHAT:
                System.out.println(Colores.VIOLETA + "[CHAT] " + m.getRemitente() + ": " + Colores.RESET
                        + m.getTexto());
                break;

            case STATS:
                System.out.println(Colores.CYAN + "\n----- ESTADÍSTICAS -----\n" + m.getTexto() + Colores.RESET);
                break;

            case INFO:
                System.out.println(Colores.AMARILLO + "[INFO] " + m.getTexto() + Colores.RESET);
                break;

            case ERROR:
                System.out.println(Colores.ROJO + "[ERROR] " + m.getTexto() + Colores.RESET);
                break;

            case FIN_PARTIDA:
                terminado.set(true);
                enPartida.set(false);
                Tablero tableroRival = m.getTablero();
                System.out.println(Colores.CYAN + "\n========== FIN DE LA PARTIDA ==========" + Colores.RESET);
                if (tableroRival != null) {
                    System.out.println(Colores.GRIS + "(se muestran los barcos del rival)" + Colores.RESET);
                    dibujarTableros(tableroRival, false);
                    tableroRival.verificarVictoria();
                }
                propio.verificarDerrota();
                System.out.println(Colores.NEGRITA + m.getTexto() + Colores.RESET);
                System.out.println(Colores.GRIS + "Presioná ENTER para salir." + Colores.RESET);
                break;

            default:
                System.out.println(m);
        }
    }

    private String textoResultado(String prefijo, Mensaje m) {
        String coord = Coordenada.aTexto(m.getFila(), m.getCol());
        String color = m.isImpacto() ? Colores.ROJO + Colores.NEGRITA : Colores.AZUL;
        String extra = m.getNombreBarco() != null ? " (" + m.getNombreBarco() + ")" : "";
        return color + prefijo + " " + coord + ": " + m.getTexto() + extra + Colores.RESET;
    }

    // ================================================================ PANTALLA

    /** Muestra el tablero propio y el del oponente lado a lado. */
    private void dibujarTableros(Tablero oponente, boolean ocultar) {
        ArrayList<String> izq = propio.generarLineas(false);
        ArrayList<String> der = oponente.generarLineas(ocultar);

        System.out.println();
        System.out.println(Colores.NEGRITA + String.format("   %-30s        %-30s",
                "TU FLOTA (" + nickName + ")", "FLOTA DE " + rival.toUpperCase()) + Colores.RESET);
        for (int i = 0; i < izq.size(); i++) {
            System.out.println(izq.get(i) + "        " + der.get(i));
        }
        System.out.println(Colores.GRIS + "   ~ agua   B barco   X impacto   O fallo" + Colores.RESET);
    }

    private void mostrarAyuda() {
        System.out.println(Colores.AMARILLO
                + "\n============== COMANDOS ==============\n"
                + "B7               Disparar a la casilla B7 (en tu turno)\n"
                + "/c mensaje       Chatear con tu rival\n"
                + "/tableros        Volver a mostrar los tableros\n"
                + "/barcos          Estado de tus barcos\n"
                + "/stats           Estadísticas e historial\n"
                + "/salir           Abandonar la partida\n"
                + "======================================" + Colores.RESET);
    }

    // ========================================================= ENVÍO/RECEPCIÓN

    private synchronized void enviar(Mensaje m) throws IOException {
        salida.writeObject(m);
        salida.reset();
        salida.flush();
    }

    private Mensaje recibir() throws IOException, ClassNotFoundException {
        return (Mensaje) entrada.readObject();
    }
}
