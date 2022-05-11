/** This code is the server that balances the workload on the computers
 * rendering an animation.
 */
package blenderparallelrendering;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static final String TARGET_DIRECTORY = "";

    public static boolean USING_ARRAY = false;

    public static int IMAGE_INDEX_IN_ARRAY;

    private static boolean isClient;
    private static ClientDisplay clientDisplay;
    private static boolean isServer;
    private static ServerDisplay serverDisplay;
    private static String serverIP;

    // The list of image indices to render; specified at startup for individual indices,
    // or in constructor for rendering a range of images.
    public static int[] imageIndexArray = {442,
        505,
        886,
        887,
        1479,
        1663};

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        // This info must be stored in a config file which we must read now.
        readClientOrServer();

        if (isServer) {
            new Thread() {
                @Override
                public void run() {

                    System.out.println("Launching server.");

                    Server server = new Server();
                    serverDisplay = new ServerDisplay();

                    int width = 1000, height = 800;

                    serverDisplay.setPreferredSize(new Dimension(width, height));
                    serverDisplay.setSize(new Dimension(width, height));

                    serverDisplay.addListener(server);
                    server.addListener(serverDisplay);

                    server.setTargetDirectory(TARGET_DIRECTORY);

                    server.createTestJob();
                    server.createTestJob();
                    server.createTestJob();
                    server.createTestJob();

                    server.run();
                    serverDisplay.revalidate();
                }
            }.start();
        }

        if (isClient) {
            new Thread() {

                @Override
                public void run() {
                    System.out.println("Launching client");
                    clientDisplay = new ClientDisplay(serverIP);
                }
            }.start();
        }
    }

    /**
     * Read local file "settings.txt" to know if this machine is a client, a
     * server, or both, and which IP address they use to communicate.
     */
    private static void readClientOrServer() {
        try {
            String filename = "src/blenderparallelrendering/settings.txt";
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            isClient = false;
            isServer = false;

            while ((line = reader.readLine()) != null) {
                if (line.equals("CLIENT")) {
                    isClient = true;
                } else if (line.equals("SERVER")) {
                    isServer = true;
                } else {
                    serverIP = line;
                }
            }
        } catch (FileNotFoundException e) {
            // Assume client in case of file not found
            System.out.println("Main - readClientOrServer: FileNotFoundException while finding config file.");
            isClient = true;
            isServer = false;
        } catch (IOException ex) {
            // Assume nothing in case of incorrect file
            System.out.println("Main - readClientOrServer: IOException while reading config file.");
            isClient = false;
            isServer = false;
        }
    }
}
