/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Respaldo;

/**
 *
 * @author skullkidms
 */
import controlador.Controlador;
import entidades.Transaccion;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;



/**
 *
 * @author skullkidms
 */
public class ManejadorInterupciones extends Thread {
    private final Socket socket;
    private final Transaccion transaccion;
    private final int contadorClientes;


    public ManejadorInterupciones(Socket socket, Transaccion transaccion, int contadorClientes) {
        this.socket = socket;
        this.transaccion = transaccion;
        this.contadorClientes = contadorClientes;
    
    }
    
    ///MODIFICAR A SWITCH CASE PARA MANEJAR LAS RECONEXIONES INTERRUMPIDAS POR LA FALLA DEL SERVIDOR PRINCIPAL
   @Override
public void run() {
    try (DataInputStream in = new DataInputStream(socket.getInputStream());
         DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {
        out.write(transaccion.getPasoActual());
        boolean continuarProceso = true;
        do {
            switch (transaccion.getPasoActual()) {
                case 0 -> {
                    enviarMensajeBienvenidaFuncion(out);
                    transaccion.avanzarPaso();
                }
                case 1 -> {
                    if (transaccion.getFuncion() == 0) { // Verifica si el dato falta
                     out.writeUTF( "\nPor favor, selecciona una función escribiendo el número correspondiente." +
                                     "\nTu elección: ");
                        int eleccionFuncion;
                        while (!Controlador.validarFuncionElegida(in.readInt(), ServidorR.sala1)) {
                            out.writeUTF("invalida");                          
                        }
                        out.writeUTF("valida");
                        eleccionFuncion = in.readInt();
                        transaccion.setFuncion(eleccionFuncion);
                        System.out.println("\nCliente " + contadorClientes + " - eligió la función " + eleccionFuncion);
                    }
                    transaccion.avanzarPaso();
                }
                case 2 -> {
                    if (transaccion.getnAsientos() == 0) { // Verifica si el dato falta
                        out.writeUTF(Controlador.mostrarDisposicionAsientos(transaccion.getFuncion(), ServidorR.sala1));                       
                        out.writeUTF("\nPor favor, ingresa la cantidad de asientos que deseas reservar: ");
                        int cantidadAsientos = in.readInt();
                        while (!Controlador.validarCantidadAsientos(cantidadAsientos, ServidorR.sala1, transaccion.getFuncion())) {
                            out.writeUTF("invalida");
                            cantidadAsientos = in.readInt();
                        }
                        transaccion.setnAsientos(cantidadAsientos);
                        out.writeUTF("valida");
                        System.out.println("\nCliente " + contadorClientes + " - quiere comprar " +
                            cantidadAsientos + " asiento(s) de la funcion " + transaccion.getFuncion());
                    }
                     
                    transaccion.avanzarPaso();
                }
                case 3 -> {
                    if (transaccion.getAsientosReservados().size()!=transaccion.getnAsientos()) { // Verifica si el dato falta
                        out.writeUTF("\nAhora, introduce la posición de los asientos que deseas reservar.\n" +
                                    "El formato a seguir es f-c (Ejemplo: 1-1).");
                        int N = transaccion.getnAsientos()-transaccion.getAsientosReservados().size();
                        for (int i = 0; i < N; i++) {
                            out.writeUTF("\nPosición del asiento " + (N-i) + ": ");
                            String posicion;
                            while (!Controlador.validarFormatoAsiento(in.readUTF())) {
                                out.writeUTF("invalida");                     
                            }
                            out.writeUTF("valida");
                            posicion = in.readUTF();
                            transaccion.agregarAsiento(posicion);
                        }
                        boolean asientosReservados = ServidorR.sala1.getFunciones().get(transaccion.getFuncion() - 1).reservarAsientos(
                        transaccion.getAsientosReservados(), transaccion.getId());
                        if (!asientosReservados) {
                            out.writeUTF("\n¡Lo sentimos! Alguno de los asientos seleccionados ya está ocupado.\n" +
                                    "Por favor, intenta con otros asientos.");
                            System.out.println("\nCliente " + contadorClientes + " - Rollback de la reserva de asientos.");
                        }
                    }
                    
                    transaccion.avanzarPaso();
                }
                case 4 -> {
                    out.writeUTF("exito");
                    ServidorR.sala1.getFunciones().get(transaccion.getFuncion() - 1).confirmarCompra(transaccion.getId());
                    out.writeUTF("¡Reserva exitosa! Gracias por tu compra.");
                    continuarProceso = false;
                }
                default -> throw new IllegalStateException("Paso desconocido: " + transaccion.getPasoActual());
            }
        } while (continuarProceso);
    } catch (IOException e) {
        System.out.println("Error en la conexión: " + e.getMessage());
    } finally {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Error al cerrar socket: " + e.getMessage());
        }
    }
}


    private void enviarMensajeBienvenidaFuncion(DataOutputStream out) throws IOException {
        out.writeUTF("¡Gracias por conectarte alservidor del cine!\n\n" +
                "Funciones disponibles:\n" +
                ServidorR.sala1.listarFunciones() +
                "\nPor favor, selecciona una función escribiendo el número correspondiente." +
                "\nTu elección: ");
    }


    public int getContadorClientes() {
        return contadorClientes;
    }
    

}