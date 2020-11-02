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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static int START_IMAGE_INDEX = 3456;
    public static int IMAGE_INDEX = START_IMAGE_INDEX;
    public static final int MAX_IMAGE_INDEX = 4855;
    public static boolean USING_ARRAY = false;
    public static int IMAGE_INDEX_IN_ARRAY;
    public static final int[] array = {3826, 3827, 3828, 3829, 3830, 3831, 3832, 3833, 3834, 3835, 3836, 3837, 3838, 3839, 3840, 3841, 3842, 3843, 3844, 3845, 3846, 3847, 3848, 3849, 3850, 3851, 3852, 3853, 3854, 3855, 3856, 3857, 3858, 3859, 3860, 3861, 3862, 3863, 3864, 3865, 3866, 3867, 3868, 3869, 3870, 3871, 3872, 3873, 3874, 3875, 3876, 3877, 3878, 3879, 3880, 3881, 3882, 3883, 3884, 3885, 3886, 3887, 3888, 3889, 3890, 3891, 3892, 3893, 3894, 3895, 3896, 3897, 3898, 3899, 3900, 3901, 3902, 3903, 3904, 3905, 3906, 3907, 3908, 3909, 3910, 3911, 3912, 3913, 3914, 3915, 3916, 3917, 3918, 3919, 3920, 3921, 3922, 3923, 3924, 3925, 3926, 3927, 3928, 3929, 3930, 3931, 3932, 3933, 3934, 3935, 3936, 3937, 3938, 3939, 3940, 3941, 3942, 3943, 3944, 3945, 3946, 3947, 3948, 3949, 3950, 3951, 3952, 3953, 3954, 3971, 3987, 4280, 4291, 4348, 4547, 4548};

    public static int NODE_NUMBER = 0;

    public static long startDate;
    public static int nbImagesNeeded;
    public static int nbImagesDone;

    public static int nbClientsConnected = 0;

    private static JFrame window;
    private static int width = 1000, height = 1000;
    private static ProgressDisplay display;

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

                // Tell the client what its number is.
                out.println("Node " + NODE_NUMBER + " ");
                NODE_NUMBER++;

                boolean loop = true;

                while (loop) {
                    int index = getNextImageIndex();
                    display.update(index, NODE_NUMBER + "", false);
                    outputLine = "server_asks_for " + index;
                    out.println(outputLine);

                    // Receive reply from client
                    do {
                        fromClient = in.readLine();
                    } while (fromClient.isEmpty());

                    // fromClient has the format "client 3, 127.0.0.42 rendered 1234"
                    String clientIP = fromClient.split(" ")[2];
                    String tab[] = clientIP.split("\\.");
                    String clientAddress = tab[2] + "." + tab[3]; // e.g. "0.42"

                    display.update(index, clientAddress, true);

                    System.out.println("Client : " + clientAddress + "; " + getETA());

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
