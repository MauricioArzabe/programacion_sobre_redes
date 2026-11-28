package ar.edu.et32.batallanaval.modelo;

import java.io.Serializable;
import java.util.ArrayList;

import ar.edu.et32.batallanaval.util.Colores;

/**
 * Tablero de 10x10 de un jugador.
 * Es Serializable para poder enviarlo por el socket al final de la partida.
 */
public class Tablero implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int TAMANIO = 10;

    // Resultados posibles de disparar()
    public static final String IMPACTO = "¡IMPACTO!";
    public static final String HUNDIDO = "¡HUNDIDO!";
    public static final String AGUA = "Agua";
    public static final String REPETIDO = "Ya se disparó en esa casilla";
    public static final String INVALIDO = "Posición inválida";

    private ArrayList<ArrayList<Celda>> tablero;
    private ArrayList<Barco> barcos;

    public Tablero() {
        inicializarTablero();
    }

    // ------------------------------------------------------------ Inicialización

    /** Crea el ArrayList de ArrayList con celdas de agua. */
    public void inicializarTablero() {
        tablero = new ArrayList<>();
        barcos = new ArrayList<>();

        for (int f = 0; f < TAMANIO; f++) {
            ArrayList<Celda> fila = new ArrayList<>();
            for (int c = 0; c < TAMANIO; c++) {
                fila.add(new Celda());
            }
            tablero.add(fila);
        }
    }

    // ------------------------------------------------------- Colocación de barcos

    public boolean colocarBarco(String nombre, int fila, int col, int longitud, boolean horizontal) {
        // 1) Que quepa en el tablero
        if (!posicionValida(fila, col) || longitud <= 0) {
            return false;
        }
        int filaFin = horizontal ? fila : fila + longitud - 1;
        int colFin = horizontal ? col + longitud - 1 : col;
        if (!posicionValida(filaFin, colFin)) {
            return false;
        }

        // 2) Que no choque con otro barco
        for (int i = 0; i < longitud; i++) {
            int f = horizontal ? fila : fila + i;
            int c = horizontal ? col + i : col;
            if (getCelda(f, c).getTipo() != TipoCelda.AGUA) {
                return false;
            }
        }

        // 3) Colocarlo
        for (int i = 0; i < longitud; i++) {
            int f = horizontal ? fila : fila + i;
            int c = horizontal ? col + i : col;
            Celda celda = getCelda(f, c);
            celda.setTipo(TipoCelda.BARCO);
            celda.setNombreBarco(nombre);
        }
        barcos.add(new Barco(nombre, longitud));
        return true;
    }

    // ----------------------------------------------------------------- Disparar

    public String disparar(int fila, int col) {
        if (!posicionValida(fila, col)) {
            return INVALIDO;
        }

        Celda celda = getCelda(fila, col);
        if (celda.isDisparada()) {
            return REPETIDO;
        }
        celda.setDisparada(true);

        if (celda.getTipo() == TipoCelda.BARCO) {
            celda.setTipo(TipoCelda.IMPACTO);
            Barco barco = buscarBarco(celda.getNombreBarco());
            if (barco != null) {
                barco.recibirImpacto();
                if (barco.estaHundido()) {
                    return HUNDIDO;
                }
            }
            return IMPACTO;
        }

        celda.setTipo(TipoCelda.FALLO);
        return AGUA;
    }

    /**
     * Usado por el cliente en el tablero del oponente (donde no conoce los barcos):
     * solo marca la casilla con el resultado que informó el servidor.
     */
    public void marcarDisparo(int fila, int col, boolean impacto) {
        if (!posicionValida(fila, col)) {
            return;
        }
        Celda celda = getCelda(fila, col);
        celda.setDisparada(true);
        celda.setTipo(impacto ? TipoCelda.IMPACTO : TipoCelda.FALLO);
    }

    // ------------------------------------------------------------- Visualización

    public void mostrarTablero(boolean ocultarBarcos) {
        for (String linea : generarLineas(ocultarBarcos)) {
            System.out.println(linea);
        }
    }

    /** Devuelve el tablero como líneas de texto (para poder mostrar dos tableros lado a lado). */
    public ArrayList<String> generarLineas(boolean ocultarBarcos) {
        ArrayList<String> lineas = new ArrayList<>();

        StringBuilder encabezado = new StringBuilder("   ");
        for (int c = 1; c <= TAMANIO; c++) {
            encabezado.append(String.format("%3d", c));
        }
        lineas.add(Colores.NEGRITA + encabezado + Colores.RESET);

        for (int f = 0; f < TAMANIO; f++) {
            StringBuilder sb = new StringBuilder();
            char letra = (char) ('A' + f);
            sb.append(Colores.NEGRITA).append(String.format(" %c ", letra)).append(Colores.RESET);
            for (int c = 0; c < TAMANIO; c++) {
                sb.append(simbolo(getCelda(f, c), ocultarBarcos));
            }
            lineas.add(sb.toString());
        }
        return lineas;
    }

    private String simbolo(Celda celda, boolean ocultarBarcos) {
        switch (celda.getTipo()) {
            case BARCO:
                return ocultarBarcos
                        ? Colores.AZUL + "  ~" + Colores.RESET
                        : Colores.VERDE + Colores.NEGRITA + "  B" + Colores.RESET;
            case IMPACTO:
                return Colores.ROJO + Colores.NEGRITA + "  X" + Colores.RESET;
            case FALLO:
                return Colores.AMARILLO + "  O" + Colores.RESET;
            case AGUA:
            default:
                return Colores.AZUL + "  ~" + Colores.RESET;
        }
    }

    /** Lista todos los barcos con su estado actual. */
    public void mostrarBarcos() {
        System.out.println(Colores.CYAN + String.format("%-14s %-9s %-9s %s",
                "BARCO", "LONGITUD", "IMPACTOS", "ESTADO") + Colores.RESET);
        for (Barco b : barcos) {
            String estado = b.estaHundido()
                    ? Colores.ROJO + "HUNDIDO" + Colores.RESET
                    : Colores.VERDE + "ACTIVO" + Colores.RESET;
            System.out.println(String.format("%-14s %-9d %-9d %s",
                    b.getNombre(), b.getLongitud(), b.getImpactos(), estado));
        }
    }

    // ------------------------------------------------- Condiciones de fin de juego

    public boolean todosLosBarcosCaidos() {
        if (barcos.isEmpty()) {
            return false;
        }
        for (Barco b : barcos) {
            if (!b.estaHundido()) {
                return false;
            }
        }
        return true;
    }

    /** Se llama sobre el tablero ENEMIGO: si están todos hundidos, ganaste. */
    public boolean verificarVictoria() {
        if (todosLosBarcosCaidos()) {
            System.out.println(Colores.VERDE + Colores.NEGRITA
                    + "\n*********************************************\n"
                    + "  ¡VICTORIA! Hundiste toda la flota enemiga\n"
                    + "*********************************************" + Colores.RESET);
            return true;
        }
        return false;
    }

    /** Se llama sobre el tablero PROPIO: si están todos hundidos, perdiste. */
    public boolean verificarDerrota() {
        if (todosLosBarcosCaidos()) {
            System.out.println(Colores.ROJO + Colores.NEGRITA
                    + "\n*********************************************\n"
                    + "  DERROTA. Hundieron toda tu flota\n"
                    + "*********************************************" + Colores.RESET);
            return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- Auxiliares

    public boolean posicionValida(int fila, int col) {
        return fila >= 0 && fila < TAMANIO && col >= 0 && col < TAMANIO;
    }

    public Celda getCelda(int fila, int col) {
        return tablero.get(fila).get(col);
    }

    public Barco buscarBarco(String nombre) {
        for (Barco b : barcos) {
            if (b.getNombre().equals(nombre)) {
                return b;
            }
        }
        return null;
    }

    public ArrayList<ArrayList<Celda>> getTablero() {
        return tablero;
    }

    public ArrayList<Barco> getBarcos() {
        return barcos;
    }
}
