package ar.edu.et32.batallanaval.modelo;

/** Barcos que cada jugador debe colocar (nombre y casillas). */
public final class Flota {

    public static final String[] NOMBRES = {
        "Portaaviones", "Acorazado", "Crucero", "Submarino", "Destructor"
    };

    public static final int[] LONGITUDES = { 5, 4, 3, 3, 2 };

    private Flota() {
    }

    public static int cantidad() {
        return NOMBRES.length;
    }
}
