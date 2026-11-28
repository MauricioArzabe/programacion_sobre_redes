package ar.edu.et32.batallanaval.servidor;

import ar.edu.et32.batallanaval.modelo.Tablero;
import ar.edu.et32.batallanaval.protocolo.Coordenada;
import ar.edu.et32.batallanaval.protocolo.Mensaje;
import ar.edu.et32.batallanaval.protocolo.TipoMensaje;
import ar.edu.et32.batallanaval.util.Colores;

/**
 * Una partida entre dos jugadores. Todos los métodos que cambian el estado son
 * synchronized: los dos hilos de los jugadores pueden llamarlos al mismo tiempo
 * y el estado se mantiene consistente.
 */
public class Partida {

    private final int id;
    private final ManejadorCliente jugador1;
    private final ManejadorCliente jugador2;
    private final Servidor servidor;

    private ManejadorCliente turno;
    private boolean terminada = false;
    private int disparos1 = 0;
    private int disparos2 = 0;

    public Partida(int id, ManejadorCliente j1, ManejadorCliente j2, Servidor servidor) {
        this.id = id;
        this.jugador1 = j1;
        this.jugador2 = j2;
        this.servidor = servidor;
        this.turno = j1; // empieza el que esperaba en la sala
    }

    public synchronized void iniciar() {
        jugador1.setPartida(this);
        jugador2.setPartida(this);

        jugador1.enviar(new Mensaje(TipoMensaje.INICIO_PARTIDA, jugador2.getNickName()));
        jugador2.enviar(new Mensaje(TipoMensaje.INICIO_PARTIDA, jugador1.getNickName()));

        System.out.println(Colores.CYAN + "[PARTIDA #" + id + "] " + jugador1.getNickName()
                + " vs " + jugador2.getNickName() + Colores.RESET);
        notificarTurno();
    }

    public synchronized void disparar(ManejadorCliente tirador, int fila, int col) {
        if (terminada) {
            tirador.enviar(new Mensaje(TipoMensaje.ERROR, "La partida ya terminó."));
            return;
        }
        if (tirador != turno) {
            tirador.enviar(new Mensaje(TipoMensaje.ERROR, "No es tu turno."));
            return;
        }

        ManejadorCliente rival = rivalDe(tirador);
        Tablero objetivo = rival.getTablero();
        String resultado = objetivo.disparar(fila, col);

        // Movimiento inválido: no pierde el turno
        if (resultado.equals(Tablero.INVALIDO) || resultado.equals(Tablero.REPETIDO)) {
            tirador.enviar(new Mensaje(TipoMensaje.ERROR, resultado + ". Volvé a disparar."));
            tirador.enviar(new Mensaje(TipoMensaje.TURNO));
            return;
        }

        if (tirador == jugador1) {
            disparos1++;
        } else {
            disparos2++;
        }

        boolean impacto = !resultado.equals(Tablero.AGUA);
        String barco = resultado.equals(Tablero.HUNDIDO) ? objetivo.getCelda(fila, col).getNombreBarco() : null;

        tirador.enviar(Mensaje.resultado(TipoMensaje.RESULTADO_DISPARO, fila, col, resultado, impacto, barco));
        rival.enviar(Mensaje.resultado(TipoMensaje.DISPARO_RIVAL, fila, col, resultado, impacto, barco));

        System.out.println(String.format("[PARTIDA #%d] %s -> %s : %s",
                id, tirador.getNickName(), Coordenada.aTexto(fila, col), resultado));

        if (objetivo.todosLosBarcosCaidos()) {
            finalizar(tirador, rival, "flota hundida");
            return;
        }

        turno = rival;
        notificarTurno();
    }

    public void chat(ManejadorCliente emisor, String texto) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        rivalDe(emisor).enviar(Mensaje.chat(emisor.getNickName(), texto));
    }

    /** Llamado cuando un jugador se desconecta: gana el otro. */
    public synchronized void abandono(ManejadorCliente jugador) {
        if (terminada) {
            return;
        }
        finalizar(rivalDe(jugador), jugador, "abandono");
    }

    private void finalizar(ManejadorCliente ganador, ManejadorCliente perdedor, String motivo) {
        terminada = true;

        String textoGanador = motivo.equals("abandono")
                ? perdedor.getNickName() + " abandonó la partida. ¡Ganaste!"
                : "¡Ganaste la partida contra " + perdedor.getNickName() + "!";
        String textoPerdedor = "Ganó " + ganador.getNickName() + ".";

        // A cada uno se le manda el tablero del rival para que vea dónde estaban los barcos
        ganador.enviar(Mensaje.fin(textoGanador, perdedor.getTablero()));
        perdedor.enviar(Mensaje.fin(textoPerdedor, ganador.getTablero()));

        int disparosGanador = ganador == jugador1 ? disparos1 : disparos2;
        servidor.getEstadisticas().registrar(id, ganador.getNickName(), perdedor.getNickName(),
                disparosGanador, motivo);
        servidor.quitarPartida(this);

        System.out.println(Colores.VERDE + "[PARTIDA #" + id + "] Ganador: " + ganador.getNickName()
                + " (" + motivo + ")" + Colores.RESET);
    }

    private void notificarTurno() {
        turno.enviar(new Mensaje(TipoMensaje.TURNO));
        rivalDe(turno).enviar(new Mensaje(TipoMensaje.ESPERA_TURNO));
    }

    private ManejadorCliente rivalDe(ManejadorCliente j) {
        return j == jugador1 ? jugador2 : jugador1;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("#%d %s vs %s (disparos %d/%d)", id,
                jugador1.getNickName(), jugador2.getNickName(), disparos1, disparos2);
    }
}
