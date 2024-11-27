package cliente;



import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * @author Christian Morga
 */


public class Cliente extends Thread{

    private static final String[] SERVIDORES = {"localhost", "localhost"}; // IPs de los servidores (puedes usar diferentes IPs o nombres de host)
    private static final int[] PUERTOS = {12346, 12347}; // Puertos para el servidor principal y respaldo
    private static String id;
    private static Socket socket=null;
    
    
    public static void main(String[] args) {
        DataInputStream in = null;
        DataOutputStream out = null;
        Scanner scanner = new Scanner(System.in);
        String servidorActivo="";
        // Intentar conectarse a uno de los servidores
        for (int i = 0; i < SERVIDORES.length; i++) {
            try {
                System.out.println("Intentando conectar a " + SERVIDORES[i] + ":" + PUERTOS[i]);
                socket = new Socket(SERVIDORES[i], PUERTOS[i]);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                servidorActivo = SERVIDORES[i] + ":" + PUERTOS[i];
                System.out.println("Conectado al servidor en " + SERVIDORES[i] + ":" + PUERTOS[i]);
                break; // Salir del bucle al conectar exitosamente
            } catch (IOException e) {
                System.err.println("No se pudo conectar a " + SERVIDORES[i] + ":" + PUERTOS[i] + " - " + e.getMessage());
            }
        }

        // Si no se pudo conectar a ningún servidor, salir del programa
        if (socket == null) {
            System.err.println("No se pudo conectar a ningún servidor. Saliendo del programa.");
            return;
        }

        try{
            // Comunicación con el servidor
            out.writeUTF("CLIENTE");
            id = in.readUTF();
            String confirmacionCompra;
            do {
                System.out.println("\nConectado al servidor de cine 🎥🍿");

                // Leer el mensaje de bienvenida del servidor
                System.out.print(in.readUTF());

                // Escribir la elección de la función al servidor
                int eleccionFuncion = scanner.nextInt();
                out.writeInt(eleccionFuncion);
                while (in.readUTF().contains("invalida")) {
                    System.out.print("\nPor favor, selecciona una función válida: ");
                    eleccionFuncion = scanner.nextInt();
                    out.writeInt(eleccionFuncion);
                }
                out.writeInt(eleccionFuncion);

                // Leer la información de los asientos disponibles de la función elegida
                System.out.print(in.readUTF());

                // Intercambio de mensajes sobre la cantidad de asientos a comprar
                System.out.print(in.readUTF());
                int cantidadAsientosPorReservar = scanner.nextInt();
                out.writeInt(cantidadAsientosPorReservar);
                while (in.readUTF().contains("invalida")) {
                    System.out.print("\nPor favor, ingresa una cantidad válida de asientos: ");
                    cantidadAsientosPorReservar = scanner.nextInt();
                    out.writeInt(cantidadAsientosPorReservar);
                }
                out.writeInt(cantidadAsientosPorReservar);

                // Solicitar los asientos a reservar
                System.out.println(in.readUTF());
                for (int i = 0; i < cantidadAsientosPorReservar; i++) {
                    System.out.print(in.readUTF());
                    String p = scanner.next();
                    out.writeUTF(p);
                    while (in.readUTF().contains("invalida")) {
                        System.out.print("\nPor favor, ingresa una posición válida de asiento: ");
                        p = scanner.next();
                        out.writeUTF(p);
                    }
                    out.writeUTF(p);
                }
                confirmacionCompra = in.readUTF();
                if (confirmacionCompra.contains("Lo sentimos")) {
                    System.out.println(confirmacionCompra);
                }
            }while (confirmacionCompra.contains("Lo sentimos"));

            // Leer la confirmación de la compra
            System.out.println(in.readUTF());

        } catch (IOException e) {
            //System.err.println("\nError en la comunicación con el servidor: " + e.getMessage());
                if(servidorActivo.equals("localhost:12346")){
                      try (Socket socket2 = new Socket(SERVIDORES[1], PUERTOS[1]);
                            DataInputStream in2 = new DataInputStream(socket2.getInputStream());
                            DataOutputStream out2 = new DataOutputStream(socket2.getOutputStream());
                            Scanner scanner2 = new Scanner(System.in)
                        ) {
                          
                        out2.writeUTF(id);
                         
                        int estado = in2.read(); // Estado inicial
                       
                        String confirmacionCompra = "";

                        do {
                            switch (estado) {
                                case 0:
                                    System.out.println("\nReconectado al servidor de cine 🎥🍿");                                  
                                    System.out.print(in2.readUTF()); // Leer mensaje de bienvenida
                                    estado++;
                                    break;
                                            
                                case 1: // Bienvenida y selección de función    
                                    System.out.println(in2.readUTF());
                                    int eleccionFuncion = scanner2.nextInt();
                                    out2.writeInt(eleccionFuncion);
                                    while (in2.readUTF().contains("invalida")) {
                                        System.out.print("\nPor favor, selecciona una función válida: ");
                                        eleccionFuncion = scanner2.nextInt();
                                        out2.writeInt(eleccionFuncion);
                                    }
                                    out2.writeInt(eleccionFuncion);                                
                                    estado++;
                                    break;
                                    

                                case 2: // Mostrar disposición de asientos y solicitar cantidad
                                    System.out.print(in2.readUTF()); // Mostrar asientos disponibles
                                    System.out.print(in2.readUTF()); // Solicitar cantidad de asientos                                
                                    int cantidadAsientosPorReservar=0;
                                    while (in2.readUTF().contains("invalida")) {
                                        System.out.print("\nPor favor, ingresa una cantidad válida de asientos: ");
                                        cantidadAsientosPorReservar = scanner2.nextInt();
                                        out2.writeInt(cantidadAsientosPorReservar);
                                    }
                                    out2.writeInt(cantidadAsientosPorReservar);
                                    estado++; // Avanzar al siguiente estado
                                    break;

                                case 3: // Solicitar asientos específicos
                                    System.out.println(in2.readUTF()); // Indicar inicio del proceso de selección de asientos
                                    for (int i = 0; i < 5; i++) {
                                        System.out.print(in2.readUTF()); // Solicitar posición del asiento
                                        String p = scanner2.next();
                                        out2.writeUTF(p);
                                        while (in2.readUTF().contains("invalida")) {
                                            System.out.print("\nPor favor, ingresa una posición válida de asiento: ");
                                            p = scanner2.next();
                                            out2.writeUTF(p);
                                        }
                                    }
                                    estado++; // Avanzar al siguiente estado
                                    break;

                                case 4: // Confirmación de la compra
                                    System.out.println("case:"+estado);
                                    confirmacionCompra = in2.readUTF();
                                    if (confirmacionCompra.contains("Lo sentimos")) {
                                        System.out.println(confirmacionCompra);
                                        estado = 1; // Reiniciar si ocurre un error
                                    } else {
                                        estado++; // Avanzar al siguiente estado
                                    }
                                    break;

                                default: // Estado final
                                    System.out.println(in2.readUTF()); // Mensaje de confirmación final
                                    break;
                            }
                        } while (confirmacionCompra.contains("Lo sentimos") || estado <= 4);



                    }catch(IOException x){
                }
            }
        }         
         finally {
            // Cerrar conexión
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
            System.out.println("\nConexión cerrada.");
        }
    }
}
