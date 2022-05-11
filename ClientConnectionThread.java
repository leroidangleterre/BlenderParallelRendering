package blenderparallelrendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author arthu
 */
public class ClientConnectionThread extends Thread {

    private String serverIP;
    private int port;
    private boolean isActive;

    private long sleepDurationInMillis = 1000;
    private long imageRenderSimulationTime = 4000;

    public ClientConnectionThread(String serverIPParam, int portParam) {
        serverIP = serverIPParam;
        port = portParam;
        isActive = false;
    }

    @Override
    public void run() {

        isActive = true;

        while (isActive) {
            Socket socket = new Socket();
            InetAddress serverAddress;
            try {

                serverAddress = InetAddress.getByName(serverIP);
                socket.connect(new InetSocketAddress(serverAddress, port));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String fromServer = in.readLine();
                String jobName = fromServer.split(" ")[1];
                if (jobName.equals("none")) {
                    System.out.println("Client: Nothing available.");
                    Thread.sleep(sleepDurationInMillis);
                } else {
                    int frame = Integer.valueOf(fromServer.split(" ")[2]);
                    System.out.println("Client received from server: " + jobName + ", frame " + frame);

                    workOnFrame(jobName, frame);

                }

            } catch (UnknownHostException ex) {
                System.out.println("Unknown Host Exception.");
                isActive = false;
            } catch (IOException ex) {
                System.out.println("IO Exception: server is unavailable.");
            } catch (InterruptedException ex) {
                System.out.println("ClientConnectionThread interrupted");
            }
        }
    }

    /**
     * Set the thread to stop before next loop.
     *
     */
    protected void flagStop() {
        System.out.println("Client being flagged to stop.");
        isActive = false;
    }

    /**
     * Render an image from the animation.
     * Check the file version and download any required material.
     */
    private void workOnFrame(String jobName, int frame) {
        System.out.println("Client working on job " + jobName + ", frame " + frame);
        try {
            Thread.sleep(imageRenderSimulationTime);
            // Check the .blend file (name and date), send a request if necessary
            // Render the image
            // Send the image to the server
            System.out.println("Client done.");
        } catch (InterruptedException ex) {
            Logger.getLogger(ClientConnectionThread.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Client error.");
        }
    }
}
