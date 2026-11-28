package ar.edu.et32.batallanaval.servidor;

import java.io.IOException;

import ar.edu.et32.batallanaval.util.Colores;

/**
 * Arranca el servidor.
 * Uso: java ... MainServidor [puerto]
 * También lee las variables de entorno PUERTO y HISTORIAL (útiles en Docker).
 */
public class MainServidor {

    public static final int PUERTO_DEFECTO = 5000;
    public static final String HISTORIAL_DEFECTO = "data/historial_partidas.txt";

    public static void main(String[] args) {
        int puerto = PUERTO_DEFECTO;
        try {
            if (args.length > 0) {
                puerto = Integer.parseInt(args[0]);
            } else if (System.getenv("PUERTO") != null) {
                puerto = Integer.parseInt(System.getenv("PUERTO"));
            }
        } catch (NumberFormatException e) {
            System.err.println("Puerto inválido, se usa " + PUERTO_DEFECTO);
        }

        String historial = System.getenv("HISTORIAL") != null ? System.getenv("HISTORIAL") : HISTORIAL_DEFECTO;

        System.out.println(Colores.CYAN + Colores.NEGRITA
                + "=========================================\n"
                + "     BATALLA NAVAL - SERVIDOR\n"
                + "=========================================" + Colores.RESET);

        try (Servidor servidor = new Servidor(puerto, historial)) {
            Runtime.getRuntime().addShutdownHook(new Thread(servidor::close, "CIERRE-SERVIDOR"));
            servidor.iniciar();
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
        }
    }
}
