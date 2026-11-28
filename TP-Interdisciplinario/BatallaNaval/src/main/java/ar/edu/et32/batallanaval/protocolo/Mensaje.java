package ar.edu.et32.batallanaval.protocolo;

import java.io.Serializable;

import ar.edu.et32.batallanaval.modelo.Tablero;

/**
 * Objeto que viaja por el socket con ObjectOutputStream / ObjectInputStream.
 * Según el tipo se usan unos campos u otros.
 */
public class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private final TipoMensaje tipo;
    private String texto;
    private String remitente;
    private int fila;
    private int col;
    private boolean horizontal;
    private boolean impacto;
    private String nombreBarco;
    private int longitud;
    private Tablero tablero;

    public Mensaje(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    public Mensaje(TipoMensaje tipo, String texto) {
        this.tipo = tipo;
        this.texto = texto;
    }

    // ---------------------------------------------- Constructores "de fábrica"

    public static Mensaje login(String nick) {
        return new Mensaje(TipoMensaje.LOGIN, nick);
    }

    public static Mensaje colocarBarco(String nombre, int longitud, int fila, int col, boolean horizontal) {
        Mensaje m = new Mensaje(TipoMensaje.COLOCAR_BARCO);
        m.nombreBarco = nombre;
        m.longitud = longitud;
        m.fila = fila;
        m.col = col;
        m.horizontal = horizontal;
        return m;
    }

    public static Mensaje disparo(int fila, int col) {
        Mensaje m = new Mensaje(TipoMensaje.DISPARO);
        m.fila = fila;
        m.col = col;
        return m;
    }

    public static Mensaje resultado(TipoMensaje tipo, int fila, int col, String texto,
            boolean impacto, String nombreBarco) {
        Mensaje m = new Mensaje(tipo, texto);
        m.fila = fila;
        m.col = col;
        m.impacto = impacto;
        m.nombreBarco = nombreBarco;
        return m;
    }

    public static Mensaje chat(String remitente, String texto) {
        Mensaje m = new Mensaje(TipoMensaje.CHAT, texto);
        m.remitente = remitente;
        return m;
    }

    public static Mensaje fin(String texto, Tablero tableroOponente) {
        Mensaje m = new Mensaje(TipoMensaje.FIN_PARTIDA, texto);
        m.tablero = tableroOponente;
        return m;
    }

    // ------------------------------------------------------------------ Getters

    public TipoMensaje getTipo() {
        return tipo;
    }

    public String getTexto() {
        return texto;
    }

    public String getRemitente() {
        return remitente;
    }

    public int getFila() {
        return fila;
    }

    public int getCol() {
        return col;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    public boolean isImpacto() {
        return impacto;
    }

    public String getNombreBarco() {
        return nombreBarco;
    }

    public int getLongitud() {
        return longitud;
    }

    public Tablero getTablero() {
        return tablero;
    }

    @Override
    public String toString() {
        return "Mensaje[" + tipo + ", texto=" + texto + ", fila=" + fila + ", col=" + col + "]";
    }
}
