/** This code is the server that balances the workload on the computers
 * rendering an animation.
 */
package blenderparallelrendering;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static final int START_IMAGE_INDEX = 1;
    public static int IMAGE_INDEX = START_IMAGE_INDEX;
    public static final int MAX_IMAGE_INDEX = 2395;
    public static boolean USING_ARRAY = false;
    public static int IMAGE_INDEX_IN_ARRAY;
    public static final int[] array = {417, 426};

    public static int NODE_NUMBER = 0;

    public static long startDate;
    public static int nbImagesNeeded;
    public static int nbImagesDone;

    public static int nbClientsConnected = 0;

    private static JFrame window;
    private static int width = 1000, height = 800;
    private static ProgressDisplay display;
    private static AverageCalculator avgCalc;

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

        window = new JFrame();

        window.setPreferredSize(new Dimension(width, height));
        window.setSize(new Dimension(width, height));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        display = new ProgressDisplay(nbImagesNeeded, USING_ARRAY);

        display.setPreferredSize(new Dimension(width, height));
        display.setSize(new Dimension(width, height));

        if (!USING_ARRAY) {
            display.setFirstImageIndex(START_IMAGE_INDEX);
        } else {
            display.setAllIndices(array);
        }

        avgCalc = new AverageCalculator();

        window.setLayout(new BorderLayout());
        JPanel buttonsPanel = new JPanel();
        setButtons(buttonsPanel);
        window.add(buttonsPanel, BorderLayout.NORTH);
        window.add(display);
        window.setVisible(true);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ConnectionHandler(clientSocket, display)).start();
        }

    }

    private static void setButtons(JPanel p) {
        JButton moreColsButton = new JButton("more cols");

        moreColsButton.addActionListener((e) -> {
            display.increaseNbCols(true);
        });

        JButton fewerColsButton = new JButton("fewer cols");
        fewerColsButton.addActionListener((e) -> {
            display.increaseNbCols(false);
        });

        p.add(moreColsButton);
        p.add(fewerColsButton);

        JButton moreEventsForAvg = new JButton("more images for average");
        moreEventsForAvg.addActionListener((e) -> {
            avgCalc.increaseSetSize();
            System.out.println(ConnectionHandler.getETA());
        });
        p.add(moreEventsForAvg);
        JButton fewerEventsForAvg = new JButton("fewer images for average");
        fewerEventsForAvg.addActionListener((e) -> {
            avgCalc.decreaseSetSize();
            System.out.println(ConnectionHandler.getETA());
        });
        p.add(fewerEventsForAvg);
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

        int result;
        if (USING_ARRAY) {
            result = array[IMAGE_INDEX];
        } else {
            result = IMAGE_INDEX;
        }
        IMAGE_INDEX++;
        return result;
    }

    private static class ConnectionHandler implements Runnable {

        String fromClient;
        Socket clientSocket;
        ProgressDisplay display;

        public ConnectionHandler(Socket s) {
            this(s, null);
        }

        private ConnectionHandler(Socket s, ProgressDisplay d) {
            clientSocket = s;
            nbClientsConnected++;
            display = d;
        }

        @Override
        public void run() {
            try {
                System.out.println("Connection accepted.");
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String outputLine;

                String clientAddress = NODE_NUMBER + "";
                NODE_NUMBER++;
                boolean loop = true;

                while (loop) {
                    int index = getNextImageIndex();
                    display.update(index, clientAddress + "", false);
                    outputLine = "server_asks_for " + index;
                    System.out.println("Server_asks_for_image " + index);
                    out.println(outputLine);

                    // Receive reply from client
                    do {
                        fromClient = in.readLine();
//                        System.out.println("line received from client: <" + fromClient + ">");
                        if (fromClient.contains("client")) {
                            // fromClient has the format "client 0x123456789, 127.0.0.42 rendered 1234"
                            clientAddress = fromClient.split(" ")[1];
//                            System.out.println("Client address:<" + clientAddress + ">");
                        }
                    } while (fromClient.isEmpty());

                    display.update(index, clientAddress, true);

                    avgCalc.add((int) System.currentTimeMillis());

                    String eta = getETA();
                    System.out.println("Client " + clientAddress + " frame " + index + " " + eta);
                    window.setTitle(eta);

                    if (nbImagesDone >= nbImagesNeeded) {
                        loop = false;
                        System.out.println("Render finished.");
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
        private static String getETA() {

            long elapsedMillisec = System.currentTimeMillis() - startDate;

            int nbRemainingImages = nbImagesNeeded - nbImagesDone;

            int averageMillisec = avgCalc.getAverage();

            int estimatedRemainingMillisec = averageMillisec * nbRemainingImages;

            String elapsed = convertSecToHMS((int) (elapsedMillisec / 1000));
            String estimatedRemaining = convertSecToHMS(estimatedRemainingMillisec / 1000);
            String estimatedTotal = convertSecToHMS((int) ((elapsedMillisec + estimatedRemainingMillisec) / 1000));

            String result = "";

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
            Date completionDate = new Date(System.currentTimeMillis() + estimatedRemainingMillisec);

            result += "Elapsed: " + elapsed
                    + " remaining: " + estimatedRemaining
                    + "est. total: " + estimatedTotal + " s "
                    + " ETC: " + completionDate
                    + "; Average: ";
            if (averageMillisec > 10000) {
                result += averageMillisec / 1000 + " s/i; ";
            } else {
                result += averageMillisec + " ms/i; ";
            }

            result += nbRemainingImages + " remaining images";

            return result;
        }

        private void receiveImageFromClient(int index, BufferedReader input) {
            System.out.println("Receiving image " + index + " from client");
            try {
                // Receive reply from client
                do {
                    System.out.println("Reading line");
                    fromClient = input.readLine();
                    System.out.println("Read " + fromClient);
                } while (fromClient.isEmpty());
                System.out.println("Image received.");
            } catch (IOException e) {
                System.out.println("Error in receiving image");
            }
        }
    }

    /**
     * Convert an amount of seconds to hours, minutes and seconds, and give the
     * result as a string formatted as "h:m:s"
     *
     * @param nbSec
     * @return a string representing that duration as hours, minutes and seconds
     */
    private static String convertSecToHMS(int nbSec) {
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
