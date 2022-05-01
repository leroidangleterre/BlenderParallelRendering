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
    public static final String TARGET_DIRECTORY = "D:\\Blender\\Dune\\png_rendered";

    public static boolean USING_ARRAY = false;
    public static int IMAGE_INDEX_IN_ARRAY;
    public static int[] array = {8, 14};

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    public static void main(String[] args) throws IOException {

        Server s = new Server();

        ProgressDisplay progressDisplay;

        progressDisplay = new ProgressDisplay();

        int width = 1000, height = 800;

        progressDisplay.setPreferredSize(new Dimension(width, height));
        progressDisplay.setSize(new Dimension(width, height));

        progressDisplay.addListener(s);
        s.addListener(progressDisplay);

        s.setTargetDirectory(TARGET_DIRECTORY);

        s.run();
    }
}
