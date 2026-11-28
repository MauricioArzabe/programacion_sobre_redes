package ar.edu.et32.batallanaval.protocolo;

/**
 * Tipos de mensajes del protocolo cliente-servidor.
 *  C->S : enviado por el cliente
 *  S->C : enviado por el servidor
 */
public enum TipoMensaje {
    // Conexión y autenticación
    LOGIN,              // C->S  texto = nickname
    LOGIN_OK,           // S->C  bienvenida

    // Posicionamiento de barcos
    COLOCAR_BARCO,      // C->S  nombreBarco, longitud, fila, col, horizontal
    BARCO_OK,           // S->C  barco aceptado
    LISTO,              // C->S  terminó de colocar la flota

    // Sala de espera
    ESPERANDO_RIVAL,    // S->C  está en la sala de espera
    INICIO_PARTIDA,     // S->C  texto = nick del oponente

    // Turnos
    TURNO,              // S->C  es tu turno
    ESPERA_TURNO,       // S->C  turno del oponente

    // Disparos
    DISPARO,            // C->S  fila, col
    RESULTADO_DISPARO,  // S->C  resultado de MI disparo (fila, col, texto, impacto, nombreBarco)
    DISPARO_RIVAL,      // S->C  el oponente me disparó (fila, col, texto, impacto, nombreBarco)

    // Extras
    CHAT,               // C->S texto | S->C texto + remitente
    STATS,              // C->S pedir | S->C texto con estadísticas
    INFO,               // S->C mensaje informativo
    ERROR,              // S->C error / movimiento inválido

    // Fin
    FIN_PARTIDA,        // S->C texto = ganador, tablero = tablero del oponente
    SALIR               // C->S abandona
}
