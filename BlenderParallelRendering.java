/** This code is the server that balances the workload on the computers
 * rendering an animation.
 */
package blenderparallelrendering;

import java.io.IOException;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static final int START_IMAGE_INDEX = 1;
    public static final int MAX_IMAGE_INDEX = 180;
    public static final String TARGET_DIRECTORY = "D:\\Blender\\Dune\\png_rendered";

//    public static int IMAGE_INDEX;
    public static boolean USING_ARRAY = false;
    public static int IMAGE_INDEX_IN_ARRAY;
    public static int[] array = {8, 14};

    private static boolean isServer;
    private static boolean isClient;

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        Server s = new Server();

        s.setTargetDirectory(TARGET_DIRECTORY);
        s.setImgArray(USING_ARRAY, array, START_IMAGE_INDEX, MAX_IMAGE_INDEX);

        new Thread() {
            public void run() {
                // Start the server.
                s.run();
            }
        }.start();
    }

//    /**
//     * Read the config file to tell if a server must run on this machine.
//     *
//     * @return true when this machine is a server.
//     */
//    public static void testServerOrClient() {
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(TARGET_DIRECTORY + "settings.txt"));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                if (line.contains("CLIENT")) {
//                    isClient = true;
//                }
//                if (line.contains("SERVER")) {
//                    isServer = true;
//                }
//            }
//        } catch (FileNotFoundException ex) {
//            System.out.println("Config file not found.");
//        } catch (IOException ex) {
//            System.out.println("IOException when reading config file.");
//        }
//    }
}
