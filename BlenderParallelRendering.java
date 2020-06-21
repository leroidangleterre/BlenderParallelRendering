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

    public static int IMAGE_INDEX = 0;
    public static final int MAX_IMAGE_INDEX = 20;

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
