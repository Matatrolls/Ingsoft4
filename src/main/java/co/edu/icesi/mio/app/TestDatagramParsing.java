package co.edu.icesi.mio.app;

import co.edu.icesi.mio.model.streaming.Datagram;

public class TestDatagramParsing {
    public static void main(String[] args) {
        String testLine = "0,28-MAY-19,513327,70,34761183,-764873683,757,2241,159,6255401365,2019-05-27 20:14:43,1069";

        System.out.println("Probando parseo de línea:");
        System.out.println(testLine);
        System.out.println();

        try {
            Datagram datagram = Datagram.fromCsvLine(testLine);
            System.out.println("✓ Parseo exitoso!");
            System.out.println(datagram);
        } catch (Exception e) {
            System.err.println("✗ Error en el parseo:");
            e.printStackTrace();
        }
    }
}
