/** This code is the server that balances the workload on the computers
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

    public static int START_IMAGE_INDEX = 3456;
    public static int IMAGE_INDEX = START_IMAGE_INDEX;
    public static final int MAX_IMAGE_INDEX = 4855;
    public static boolean USING_ARRAY = false;
    public static final int[] array = {
        4487, 4496, 4503, 4504, 4511, 4513, 4520, 4523,
        4528, 4535, 4537, 4545, 4547, 4553, 4559, 4562, 4569, 4572, 4577, 4583,
        4585, 4592, 4594, 4600, 4602, 4610, 4617, 4625, 4632, 4639, 4646, 4652,
        4660, 4668, 4676, 4683, 4690, 4698, 4706, 4713, 4714, 4856};

    public static int NODE_NUMBER = 0;

    public static long startDate;
    public static int nbImagesNeeded;
    public static int nbImagesDone;

    public static int nbClientsConnected = 0;

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        int port = 65432;
        ServerSocket serverSocket = new ServerSocket(port);
        InetAddress localhost = InetAddress.getLocalHost();
        System.out.println("address: " + localhost.getHostAddress());

        startDate = System.currentTimeMillis();

        nbImagesNeeded = MAX_IMAGE_INDEX - START_IMAGE_INDEX + 1;
        System.out.println("Rendering " + nbImagesNeeded + " images.");

        // When the array is used, we start counting at zero.
        if (USING_ARRAY) {
            IMAGE_INDEX = 0;
            nbImagesNeeded = array.length;
        }

        nbImagesDone = 0;

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ConnectionHandler(clientSocket)).start();
        }

    }

    /**
     * Increment the index and return the new value. Synchronization makes sure
     * no image index is sent twice.
     *
     * @return the image index that the thread must render.
     */
    public static synchronized int getNextImageIndex() {

        // One more image is being processed.
        nbImagesDone++; // TODO: this should be done when an image is confirmed.

        IMAGE_INDEX++;
        if (USING_ARRAY) {
            return array[IMAGE_INDEX - 1];
        }
        return IMAGE_INDEX;
    }

    private static class ConnectionHandler implements Runnable {

        String fromClient;
        Socket clientSocket;

        public ConnectionHandler(Socket s) {
            clientSocket = s;
            nbClientsConnected++;
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

//                    System.out.println(fromClient);
                    System.out.println(getETA());

                    if (IMAGE_INDEX > MAX_IMAGE_INDEX) {
                        loop = false;
                    }
                }
                out.println("END");
            } catch (IOException e) {
                System.out.println("Error in handling connection");
                nbClientsConnected--;
            }
        }

        /**
         * Estimate the remaining time, using the number of images needed and
         * the number of images already processed.
         *
         * @return a String representing the ETA.
         */
        private String getETA() {

            long elapsedMillisec = System.currentTimeMillis() - startDate;

            int nbRemainingImages = nbImagesNeeded - nbImagesDone;

            int averageMillisec = (int) (elapsedMillisec / nbImagesDone);

            int estimatedRemainingMillisec = averageMillisec * nbRemainingImages;

            String result = "Elapsed: " + elapsedMillisec / 1000 + " s, "
                    + "remaining: " + estimatedRemainingMillisec / 1000 + " s; "
                    + "est. total: " + (elapsedMillisec + estimatedRemainingMillisec) / 1000 + " s "
                    + "; Average: ";
            if (averageMillisec > 10000) {
                result += averageMillisec / 1000 + " s/i; ";
            } else {
                result += averageMillisec + " ms/i; ";
            }

            result += nbRemainingImages + " remaining images";

            return result;
        }
    }

    private static String getHMS(int nbSec) {
        String result = "";
        int nbHours = nbSec / 3600;
        if (nbHours > 0) {
            result += nbHours + ":";
        }

        int nbMin = (nbSec - 3600 * nbHours) / 60;
        if (nbSec > 60) {
            if (nbMin < 10) {
                result += "0";
            }
            result += nbMin + ":";
        }
        nbSec = nbSec - 3600 * nbHours - 60 * nbMin;
        if (nbSec < 10) {
            result += "0";
        }
        result += nbSec + "\"";
        return result;
    }

}
