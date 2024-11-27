package servidor;

import controlador.Controlador;
import entidades.Sala;
import entidades.Transaccion;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Servidor {

    private static final int PUERTO = 12346; // Puerto principal
    private static int contadorClientes = 0;
    private static final ExecutorService poolHilos = Executors.newFixedThreadPool(10);
    public static Sala sala1 = Controlador.instanciarSala1();
    private static final Map<String, Transaccion> transaccionesActivas = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        HeartbeatSender heartbeatSender = new HeartbeatSender();
        heartbeatSender.start();

        System.out.println("\nServidor principal esperando conexiones en el puerto " + PUERTO + "...");

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream in = new  DataInputStream(socket.getInputStream());
                String id= in.readUTF();                  
                iniciarConexionClienteHilo(socket);
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor principal: " + e.getMessage());
        } finally {
            heartbeatSender.detener();
        }
    }

    private static void iniciarConexionClienteHilo(Socket socket) throws IOException {
        synchronized (Servidor.class) {
            contadorClientes++;
        }
        String idTransaccion = Controlador.generarIdTransaccion();
        System.out.println("\nNueva conexión - Cliente: " + contadorClientes + " - idTransaccion: " + idTransaccion);
        Transaccion nuevaTransaccion = new Transaccion(idTransaccion);
        transaccionesActivas.put(idTransaccion, nuevaTransaccion);
        poolHilos.execute(new ServidorHilo(socket, nuevaTransaccion, contadorClientes));
    }

    public static synchronized void mensajeClienteDesconectado(String idTransaccion, int contadorClientes) {
        System.out.println("\nConexión finalizada - Cliente: " + contadorClientes + " - idTransaccion: " + idTransaccion);
        transaccionesActivas.remove(idTransaccion);
    }
    
    
}

// Hilo para enviar heartbeats al servidor de respaldo
class HeartbeatSender extends Thread {
    private boolean enviando = true;

    @Override
    public void run() {
        try (Socket socketRespaldo = new Socket("localhost", 12348);
             DataOutputStream out = new DataOutputStream(socketRespaldo.getOutputStream())) {

            while (enviando) {
                try {
                    out.writeUTF("HEARTBEAT");
                    out.flush();
                    //System.out.println("Heartbeat enviado al respaldo.");
                    Thread.sleep(5000); // Envía un heartbeat cada 5 segundos
                } catch (IOException e) {
                   // System.out.println("Error enviando heartbeat al respaldo: " + e.getMessage());
                    break; // Salir del bucle si ocurre un error
                } catch (InterruptedException ex) {
                    Logger.getLogger(HeartbeatSender.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        } catch (IOException e) {
            System.out.println("No se pudo conectar al servidor de respaldo: " + e.getMessage());
        }
    }

    public void detener() {
        enviando = false;
    }
}
