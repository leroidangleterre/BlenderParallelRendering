/** This code is the server that dispatches workload on the computers
 * rendering an animation.
 */
package blenderparallelrendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static int IMAGE_INDEX = 4483;
    public static final int MAX_IMAGE_INDEX = 4856;
//    public static final int[] array = {
//        3476, 3477, 3481, 3488, 3498, 3509, 3521, 3532, 3544, 3802, 3827, 3880,
//        3956, 3978, 4149, 4161, 4162, 4163, 4164, 4165, 4166, 4167, 4168, 4169,
//        4170, 4172, 4173, 4174, 4175, 4176, 4177, 4178, 4179, 4180, 4181, 4856};

    public static int NODE_NUMBER = 0;

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        int port = 65432;
        ServerSocket serverSocket = new ServerSocket(port);
        InetAddress localhost = InetAddress.getLocalHost();
        System.out.println("address: " + localhost.getHostAddress());

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ConnectionHandler(clientSocket)).start();
        }

    }

    /**
     * Increment the index and return the new value.
     * Synchronization makes sure two threads will receive different values.
     *
     * @return the image index that the thread must render.
     */
    public static synchronized int getNextImageIndex() {
        IMAGE_INDEX++;
//        return array[IMAGE_INDEX - 1];
        return IMAGE_INDEX;
    }

    private static class ConnectionHandler implements Runnable {

        String fromClient;
        Socket clientSocket;

        public ConnectionHandler(Socket s) {
            clientSocket = s;
        }

        @Override
        public void run() {
            try {
                System.out.println("Connection accepted.");
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String outputLine;

                // Tell the client what its number is.
                out.println("Node " + NODE_NUMBER + " ");
                NODE_NUMBER++;

                boolean loop = true;

                while (loop) {
                    int index = getNextImageIndex();
                    outputLine = "server_asks_for " + index;
                    out.println(outputLine);
                    do {
                        fromClient = in.readLine();
                    } while (fromClient.isEmpty());

                    System.out.println("    client replied: <" + fromClient + ">");

                    if (IMAGE_INDEX > MAX_IMAGE_INDEX) {
                        loop = false;
                    }
                }
                out.println("END");
            } catch (IOException e) {
                System.out.println("Error in handling connection");
            }
        }
    }

}
