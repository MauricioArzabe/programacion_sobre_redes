package ar.edu.et32.batallanaval.modelo;

import java.io.Serializable;

/** Un barco de la flota. */
public class Barco implements Serializable {

    private static final long serialVersionUID = 1L;

    private String nombre;
    private int longitud;
    private int impactos;

    public Barco(String nombre, int longitud) {
        this.nombre = nombre;
        this.longitud = longitud;
        this.impactos = 0;
    }

    /** Incrementa el contador de impactos. */
    public void recibirImpacto() {
        impactos++;
    }

    /** true si impactos >= longitud. */
    public boolean estaHundido() {
        return impactos >= longitud;
    }

    public String getNombre() {
        return nombre;
    }

    public int getLongitud() {
        return longitud;
    }

    public int getImpactos() {
        return impactos;
    }
}
