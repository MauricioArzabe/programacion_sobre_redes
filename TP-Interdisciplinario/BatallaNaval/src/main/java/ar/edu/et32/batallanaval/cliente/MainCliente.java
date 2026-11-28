package ar.edu.et32.batallanaval.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ar.edu.et32.batallanaval.util.Colores;

/**
 * Arranca el cliente.
 * Uso: java ... MainCliente [ip] [puerto]   (si no se pasan, se preguntan por consola)
 */
public class MainCliente {

    private static final String HOST_DEFECTO = "localhost";
    private static final int PUERTO_DEFECTO = 5000;

    public static void main(String[] args) {
        BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in));

        System.out.println(Colores.CYAN + Colores.NEGRITA
                + "=========================================\n"
                + "        BATALLA NAVAL - CLIENTE\n"
                + "=========================================" + Colores.RESET);

        try {
            String host;
            int puerto;

            if (args.length >= 2) {
                host = args[0];
                puerto = Integer.parseInt(args[1]);
            } else {
                System.out.print("IP del servidor [" + HOST_DEFECTO + "]: ");
                host = leer(teclado, HOST_DEFECTO);
                System.out.print("Puerto [" + PUERTO_DEFECTO + "]: ");
                puerto = Integer.parseInt(leer(teclado, String.valueOf(PUERTO_DEFECTO)));
            }

            new ClienteJuego(host, puerto, teclado).jugar();

        } catch (NumberFormatException e) {
            System.err.println(Colores.ROJO + "El puerto debe ser un número." + Colores.RESET);
        } catch (IOException e) {
            System.err.println(Colores.ROJO + "Error leyendo el teclado: " + e.getMessage() + Colores.RESET);
        }
    }

    private static String leer(BufferedReader teclado, String porDefecto) throws IOException {
        String linea = teclado.readLine();
        return (linea == null || linea.isBlank()) ? porDefecto : linea.trim();
    }
}
