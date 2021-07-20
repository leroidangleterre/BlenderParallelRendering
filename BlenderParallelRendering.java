/** This code is the server that balances the workload on the computers
 * rendering an animation.
 */
package blenderparallelrendering;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static final int START_IMAGE_INDEX = 1;
    public static int IMAGE_INDEX;
    public static final int MAX_IMAGE_INDEX = 1200;
    public static boolean USING_ARRAY = false;
    public static int IMAGE_INDEX_IN_ARRAY;
    public static int[] array = {8, 14};

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

        // When the array is used, we start counting at zero.
        if (USING_ARRAY) {
            nbImagesNeeded = array.length;
        } else {
            // We have the first and last image indices, we create an array of consecutive indices.
            array = new int[MAX_IMAGE_INDEX - START_IMAGE_INDEX + 1];
            for (int indexInArray = 0; indexInArray < array.length; indexInArray++) {
                array[indexInArray] = START_IMAGE_INDEX + indexInArray;
            }
            nbImagesNeeded = MAX_IMAGE_INDEX - START_IMAGE_INDEX + 1;
        }

        // Now that the array is set, we always start at the first image index in the array.
        IMAGE_INDEX = 0;
        System.out.println("Rendering " + nbImagesNeeded + " images.");

        nbImagesDone = 0;

        window = new JFrame();

        window.setPreferredSize(new Dimension(width, height));
        window.setSize(new Dimension(width, height));
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        display = new ProgressDisplay(array);

        display.setPreferredSize(new Dimension(width, height));
        display.setSize(new Dimension(width, height));

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

        JTextField resetImageTextField = new JTextField();
        int textFieldWidth = 8;
        resetImageTextField.setColumns(textFieldWidth);
        JButton resetImageButton = new JButton("Invalidate image");
        resetImageButton.addActionListener((e) -> {
            try {
                int imageToReset = Integer.valueOf(resetImageTextField.getText());
                System.out.println("Invalidate image " + imageToReset);
                invalidateImage(imageToReset);
            } catch (NumberFormatException ex) {
                System.out.println("Cannot convert " + resetImageTextField.getText() + " to an integer.");
            }
        });

        p.add(resetImageTextField);
        p.add(resetImageButton);
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

        result = array[IMAGE_INDEX];
        IMAGE_INDEX++;
        return result;
    }

    /**
     * The image with the given index must be removed from the list, marked 'not
     * rendered yet', and put back at the end of the list.
     *
     * @param imageToReset the index of the image that is being reset
     */
    private static void invalidateImage(int imageToReset) {

        int rank = display.findRankOfImage(imageToReset);
        // Shift one step to the left all the images that were after the reset image.
        for (int index = rank; index < array.length - 1; index++) {
            array[index] = array[index + 1];
        }
        // Place the chosen image at the end.
        array[array.length - 1] = imageToReset;
        display.invalidateImage(imageToReset);

        IMAGE_INDEX--;
        nbImagesDone--;
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
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String outputLine;

                String clientAddress = NODE_NUMBER + "";
                NODE_NUMBER++;
                boolean loop = true;
                int imageSize;

                int bufferSize = 4096;

                int index = getNextImageIndex();
                display.update(index, clientAddress + "", false);
                outputLine = "server_asks_for " + index;
                // ---------------- send MESSAGE 1
                out.println(outputLine);

                // Receive reply from client
                // ---------------- receive MESSAGE 2
                fromClient = in.readLine();

                clientAddress = fromClient.split(" ")[1];

                if (fromClient.startsWith("server")) {
                    // The server already wrote the image, nothing more to do.

                } else {
                    // We must receive the image from the client.

                    System.out.println("" + fromClient);
                    // fromClient has the format "client 0x123456789, 127.0.0.42 rendered 1234 size 40000"
                    imageSize = Integer.parseInt(fromClient.split(" ")[7]);

                    // Actually receive the image and create the file
                    String targetDirectory = "C:\\Users\\arthu\\Documents\\Blender\\May The Fourth 2021\\png_rendered\\";
                    File targetFile = new File(targetDirectory + "final_render" + index + ".png");

                    Path finalPath = targetFile.toPath();

                    DataInputStream dataStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
                    FileOutputStream fileStream = new FileOutputStream(targetFile);
                    byte[] bytes = new byte[bufferSize];

                    // ----------------- send MESSAGE 3
                    out.println("Server is ready for image");

                    int offset = 0;
                    int step = 0;
                    try {
                        int count = 0;
                        // -------------- receive DATA
                        while ((count = dataStream.read(bytes)) != -1) {
                            fileStream.write(bytes, 0, count);
                            offset += count;
                            step++;
                        }
                    } catch (java.net.SocketException e) {
                        // TODO: what do we do in case of a socket exception ?
                    } catch (EOFException e) {
                        System.out.println("" + e);
                    } catch (IOException e) {
                        System.out.println("" + e);
                    }

                    fileStream.close();
                    dataStream.close();
                }
                display.update(index, clientAddress, true);

                avgCalc.add((int) System.currentTimeMillis());

                String eta = getETA();
                System.out.println("Node " + clientAddress + " f<" + index + "> " + eta);
                window.setTitle(eta);

                if (nbImagesDone >= nbImagesNeeded) {
                    loop = false;
                    System.out.println("Render finished.");
                }
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
                    + " ETC: " + completionDate.toString().substring(4, 19)
                    + "; Avg: ";
            if (averageMillisec > 10000) {
                result += averageMillisec / 1000 + " s/i; ";
            } else {
                result += averageMillisec + " ms/i; ";
            }

            result += nbRemainingImages + " remaining images";

            return result;
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
