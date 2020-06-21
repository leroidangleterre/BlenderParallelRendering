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

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        int port = 65432;
        ServerSocket serverSocket = new ServerSocket(port);
        InetAddress localhost = InetAddress.getLocalHost();
        System.out.println("address: " + localhost.getHostAddress());

        Socket clientSocket = serverSocket.accept();
        new Thread(new ConnectionHandler(clientSocket)).start();

    }

    private static class ConnectionHandler implements Runnable {

        String fromClient;
        int renderIndex = 1;
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
                while (renderIndex < 40) {
                    outputLine = "server_asks_for " + renderIndex;
                    System.out.println("Sending request for " + outputLine);
                    out.println(outputLine);
                    do {
                        fromClient = in.readLine();
                    } while (fromClient.isEmpty());

                    System.out.println("    client replied: <" + fromClient + ">");
                    renderIndex++;

                }
            } catch (IOException e) {
                System.out.println("Error in handling connection");
            }
        }
    }

}
