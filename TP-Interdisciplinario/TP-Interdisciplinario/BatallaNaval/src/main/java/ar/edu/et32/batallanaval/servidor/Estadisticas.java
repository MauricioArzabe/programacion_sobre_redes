package ar.edu.et32.batallanaval.servidor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

/**
 * Historial de partidas en un archivo de texto.
 * Formato de cada línea:  fecha;idPartida;ganador;perdedor;disparosGanador;motivo
 */
public class Estadisticas {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final File archivo;

    public Estadisticas(String ruta) {
        archivo = new File(ruta);
        File carpeta = archivo.getAbsoluteFile().getParentFile();
        if (carpeta != null && !carpeta.exists()) {
            carpeta.mkdirs();
        }
    }

    public synchronized void registrar(int id, String ganador, String perdedor, int disparos, String motivo) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo, true))) {
            pw.println(String.join(";", LocalDateTime.now().format(FORMATO), String.valueOf(id),
                    ganador, perdedor, String.valueOf(disparos), motivo));
        } catch (IOException e) {
            System.err.println("No se pudo guardar el historial: " + e.getMessage());
        }
    }

    /** Arma el texto con las estadísticas de un jugador y las últimas partidas. */
    public synchronized String resumen(String nick) {
        ArrayList<String[]> partidas = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                String[] datos = linea.split(";");
                if (datos.length == 6) {
                    partidas.add(datos);
                }
            }
        } catch (FileNotFoundException e) {
            return "Todavía no se jugó ninguna partida.";
        } catch (IOException e) {
            return "No se pudo leer el historial: " + e.getMessage();
        }

        int ganadas = 0;
        int perdidas = 0;
        for (String[] p : partidas) {
            if (p[2].equalsIgnoreCase(nick)) {
                ganadas++;
            } else if (p[3].equalsIgnoreCase(nick)) {
                perdidas++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Partidas jugadas en el servidor: %d%n", partidas.size()));
        sb.append(String.format("%s -> ganadas: %d | perdidas: %d%n", nick, ganadas, perdidas));
        sb.append("Últimas partidas:\n");

        int desde = Math.max(0, partidas.size() - 5);
        for (int i = partidas.size() - 1; i >= desde; i--) {
            String[] p = partidas.get(i);
            sb.append(String.format("  %s  #%-3s %-12s venció a %-12s (%s disparos, %s)%n",
                    p[0], p[1], p[2], p[3], p[4], p[5]));
        }
        return sb.toString();
    }
}
