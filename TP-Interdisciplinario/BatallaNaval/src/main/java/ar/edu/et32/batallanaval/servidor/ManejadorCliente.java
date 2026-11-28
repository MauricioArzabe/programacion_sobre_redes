package ar.edu.et32.batallanaval.servidor;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;

import ar.edu.et32.batallanaval.modelo.Flota;
import ar.edu.et32.batallanaval.modelo.Tablero;
import ar.edu.et32.batallanaval.protocolo.Mensaje;
import ar.edu.et32.batallanaval.protocolo.TipoMensaje;
import ar.edu.et32.batallanaval.util.Colores;

/**
 * Atiende a UN cliente en su propio hilo (Runnable).
 * Lee objetos Mensaje y los procesa.
 */
public class ManejadorCliente implements Runnable {

    private final Socket socket;
    private final Servidor servidor;
    private ObjectOutputStream salida;
    private ObjectInputStream entrada;

    private final AtomicBoolean conectado = new AtomicBoolean(true);
    private String nickName;

    private final Tablero tablero = new Tablero();
    private int barcosColocados = 0;
    private boolean listo = false;
    private volatile Partida partida;

    public ManejadorCliente(Socket socket, Servidor servidor) {
        this.socket = socket;
        this.servidor = servidor;
    }

    @Override
    public void run() {
        try {
            // Primero el ObjectOutputStream + flush, para que el cliente pueda crear su ObjectInputStream
            salida = new ObjectOutputStream(socket.getOutputStream());
            salida.flush();
            entrada = new ObjectInputStream(socket.getInputStream());

            if (!autenticar()) {
                return;
            }

            while (conectado.get()) {
                Mensaje msg = (Mensaje) entrada.readObject();
                if (!procesar(msg)) {
                    break;
                }
            }

        } catch (EOFException | SocketException e) {
            // El cliente cerró la conexión
        } catch (IOException e) {
            System.err.println(Colores.ROJO + "[ERROR E/S] " + nickName + ": " + e.getMessage() + Colores.RESET);
        } catch (ClassNotFoundException | ClassCastException e) {
            System.err.println(Colores.ROJO + "[ERROR] Mensaje desconocido de " + nickName + Colores.RESET);
        } finally {
            cerrar();
        }
    }

    /** Conexión inicial: espera un LOGIN con un nickname válido y libre. */
    private boolean autenticar() throws IOException, ClassNotFoundException {
        while (conectado.get()) {
            Mensaje msg = (Mensaje) entrada.readObject();

            if (msg.getTipo() == TipoMensaje.SALIR) {
                return false;
            }
            if (msg.getTipo() != TipoMensaje.LOGIN) {
                enviar(new Mensaje(TipoMensaje.ERROR, "Primero tenés que identificarte."));
                continue;
            }

            String nick = msg.getTexto() == null ? "" : msg.getTexto().trim();
            String error = servidor.registrar(nick, this);
            if (error != null) {
                enviar(new Mensaje(TipoMensaje.ERROR, error));
                continue;
            }

            nickName = nick;
            System.out.println(Colores.VERDE + "[CONECTADO] " + nickName + " desde "
                    + socket.getInetAddress().getHostAddress() + Colores.RESET);
            enviar(new Mensaje(TipoMensaje.LOGIN_OK, "Bienvenido " + nickName + "!"));
            return true;
        }
        return false;
    }

    /** Devuelve false si el cliente pidió salir. */
    private boolean procesar(Mensaje msg) {
        switch (msg.getTipo()) {
            case COLOCAR_BARCO:
                colocarBarco(msg);
                break;

            case LISTO:
                if (listo) {
                    enviar(new Mensaje(TipoMensaje.ERROR, "Ya estás en la sala de espera."));
                } else if (barcosColocados < Flota.cantidad()) {
                    enviar(new Mensaje(TipoMensaje.ERROR, "Faltan colocar barcos."));
                } else {
                    listo = true;
                    servidor.getSala().ingresar(this);
                }
                break;

            case DISPARO:
                Partida p = partida;
                if (p == null) {
                    enviar(new Mensaje(TipoMensaje.ERROR, "Todavía no estás en una partida."));
                } else {
                    p.disparar(this, msg.getFila(), msg.getCol());
                }
                break;

            case CHAT:
                Partida pc = partida;
                if (pc == null) {
                    enviar(new Mensaje(TipoMensaje.ERROR, "No tenés rival para chatear todavía."));
                } else {
                    pc.chat(this, msg.getTexto());
                }
                break;

            case STATS:
                enviar(new Mensaje(TipoMensaje.STATS, servidor.getEstadisticas().resumen(nickName)));
                break;

            case SALIR:
                return false;

            default:
                enviar(new Mensaje(TipoMensaje.ERROR, "Mensaje no esperado: " + msg.getTipo()));
        }
        return true;
    }

    /** El servidor valida cada barco en SU copia del tablero del jugador. */
    private void colocarBarco(Mensaje msg) {
        if (listo || barcosColocados >= Flota.cantidad()) {
            enviar(new Mensaje(TipoMensaje.ERROR, "Ya colocaste toda tu flota."));
            return;
        }

        String esperado = Flota.NOMBRES[barcosColocados];
        int largo = Flota.LONGITUDES[barcosColocados];
        if (!esperado.equals(msg.getNombreBarco()) || largo != msg.getLongitud()) {
            enviar(new Mensaje(TipoMensaje.ERROR, "Se esperaba el " + esperado + " (" + largo + ")."));
            return;
        }

        if (tablero.colocarBarco(esperado, msg.getFila(), msg.getCol(), largo, msg.isHorizontal())) {
            barcosColocados++;
            enviar(new Mensaje(TipoMensaje.BARCO_OK, esperado + " colocado."));
        } else {
            enviar(new Mensaje(TipoMensaje.ERROR, "Posición inválida para el " + esperado + "."));
        }
    }

    /**
     * Envía un objeto al cliente. Es synchronized porque pueden escribir varios hilos
     * (el propio y el del rival a través de la Partida).
     * Si falla NO llama a cerrar() (para no generar bloqueos): solo cierra el socket
     * y el hilo lector termina y hace la limpieza.
     */
    public synchronized boolean enviar(Mensaje msg) {
        if (!conectado.get() || salida == null) {
            return false;
        }
        try {
            salida.writeObject(msg);
            salida.reset();   // evita que el stream reutilice objetos ya enviados
            salida.flush();
            return true;
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // ya estaba cerrado
            }
            return false;
        }
    }

    public void cerrar() {
        if (!conectado.compareAndSet(true, false)) {
            return;
        }
        try {
            socket.close();
        } catch (IOException e) {
            System.err.println("Error cerrando el socket de " + nickName);
        }
        servidor.desconectar(this);
    }

    public String getNickName() {
        return nickName;
    }

    public Tablero getTablero() {
        return tablero;
    }

    public Partida getPartida() {
        return partida;
    }

    public void setPartida(Partida partida) {
        this.partida = partida;
    }

    public boolean isConectado() {
        return conectado.get();
    }
}
