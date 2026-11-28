package ar.edu.et32.batallanaval.modelo;

/** Estados posibles de una casilla del tablero. */
public enum TipoCelda {
    AGUA,      // Celda sin barco
    BARCO,     // Celda con parte de un barco
    IMPACTO,   // Celda con barco que fue disparada
    FALLO      // Celda de agua que fue disparada
}
