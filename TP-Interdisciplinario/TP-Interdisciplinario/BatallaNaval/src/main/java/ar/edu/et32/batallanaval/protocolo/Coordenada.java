package ar.edu.et32.batallanaval.protocolo;

import ar.edu.et32.batallanaval.modelo.Tablero;

/** Conversión entre texto ("B7") e índices (fila 1, col 6). */
public final class Coordenada {

    private Coordenada() {
    }

    /**
     * Convierte "B7", "b7" o "B 7" en {fila, col}. Devuelve null si no es válida.
     */
    public static int[] parsear(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toUpperCase().replace(" ", "");
        if (limpio.length() < 2 || limpio.length() > 3) {
            return null;
        }

        int fila = limpio.charAt(0) - 'A';
        int col;
        try {
            col = Integer.parseInt(limpio.substring(1)) - 1;
        } catch (NumberFormatException e) {
            return null;
        }

        if (fila < 0 || fila >= Tablero.TAMANIO || col < 0 || col >= Tablero.TAMANIO) {
            return null;
        }
        return new int[] { fila, col };
    }

    public static String aTexto(int fila, int col) {
        return String.valueOf((char) ('A' + fila)) + (col + 1);
    }
}
