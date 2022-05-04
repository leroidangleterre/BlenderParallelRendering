/** This code is the server that balances the workload on the computers
 * rendering an animation.
 */
package blenderparallelrendering;

import java.awt.Dimension;
import java.io.IOException;

/**
 *
 * @author arthurmanoha
 */
public class BlenderParallelRendering {

    public static final int START_IMAGE_INDEX = 296;
    public static final int MAX_IMAGE_INDEX = 437;
    public static final String TARGET_DIRECTORY = "";

    public static boolean USING_ARRAY = false;

    public static int IMAGE_INDEX_IN_ARRAY;

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

        Server server = new Server();

        ProgressDisplay progressDisplay;
        progressDisplay = new ProgressDisplay();

        int width = 1000, height = 800;

        progressDisplay.setPreferredSize(new Dimension(width, height));
        progressDisplay.setSize(new Dimension(width, height));

        progressDisplay.addListener(server);
        server.addListener(progressDisplay);

        server.setTargetDirectory(TARGET_DIRECTORY);

        server.createTestJob();

        server.run();
    }
}
