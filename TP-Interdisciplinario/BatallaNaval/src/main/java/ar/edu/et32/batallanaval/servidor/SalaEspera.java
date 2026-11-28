package ar.edu.et32.batallanaval.servidor;

import java.util.ArrayList;

import ar.edu.et32.batallanaval.protocolo.Mensaje;
import ar.edu.et32.batallanaval.protocolo.TipoMensaje;
import ar.edu.et32.batallanaval.util.Colores;

/**
 * Sala de espera: empareja a los jugadores de a dos.
 * synchronized para que dos jugadores que llegan juntos no queden en la misma "silla".
 */
public class SalaEspera {

    private final ArrayList<ManejadorCliente> esperando = new ArrayList<>();
    private final Servidor servidor;

    public SalaEspera(Servidor servidor) {
        this.servidor = servidor;
    }

    public synchronized void ingresar(ManejadorCliente jugador) {
        // Se descartan los que se desconectaron mientras esperaban
        esperando.removeIf(j -> !j.isConectado());

        if (esperando.isEmpty()) {
            esperando.add(jugador);
            jugador.enviar(new Mensaje(TipoMensaje.ESPERANDO_RIVAL,
                    "Estás en la sala de espera. Esperando un rival..."));
            System.out.println(Colores.AMARILLO + "[SALA] " + jugador.getNickName() + " espera rival" + Colores.RESET);
            return;
        }

        ManejadorCliente rival = esperando.remove(0);
        servidor.crearPartida(rival, jugador);
    }

    public synchronized void quitar(ManejadorCliente jugador) {
        esperando.remove(jugador);
    }

    public synchronized int cantidad() {
        return esperando.size();
    }
}
