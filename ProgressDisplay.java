/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blenderparallelrendering;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JPanel;

/**
 *
 * @author arthurmanoha
 */
public class ProgressDisplay extends JPanel {

    // This tab represents the distribution of the clients that rendered the images.
    // It contains the id of the client, or -1 if the image was not rendered yet.
    String[] clientPortTab;
    int[] imageIndexTab;
    private final HashMap<String, Color> colors;
    private final ArrayList<Color> availableColors;

    int nbLines, nbColumns;
    int newHeight;

    int currentIndex;
    boolean useArray;

    private static final String NOT_STARTED = "NOT_STARTED";
    private static final String NOT_FINISHED = "NOT_FINISHED";

    public ProgressDisplay(int nbImages, boolean useArrayParam) {

        // Hashmap key: string value of client; value: allocated color.
        colors = new HashMap<>();
        availableColors = new ArrayList<>();
        availableColors.add(Color.red);
        availableColors.add(Color.blue);
        availableColors.add(Color.yellow);
        availableColors.add(Color.green);
        availableColors.add(Color.cyan);

        clientPortTab = new String[nbImages];
        imageIndexTab = new int[nbImages];
        for (int i = 0; i < nbImages; i++) {
            clientPortTab[i] = NOT_STARTED;
            imageIndexTab[i] = -1;
        }

        nbColumns = (int) Math.sqrt(clientPortTab.length);
        nbLines = clientPortTab.length / nbColumns;
        if (nbColumns * nbColumns != clientPortTab.length) {
            // Not a perfect square, need to add a line.
            nbLines++;
        }

        computeHeight();

        repaint();
    }

    /**
     * Tell the progress display that the n-th image has been rendered.
     *
     * @param renderedImageIndex the index of the image, starting from ZERO
     * (i.e. if we render images 3255 through 3265, then values range from 0 to
     * 10);
     * @param clientAddress the id of the client that rendered said image.
     * @param finished true when the image is confirmed done, false when it is
     * still being computed.
     */
    public void update(int renderedImageIndex, String clientAddress, boolean finished) {

        String clientPort;

        String[] split = clientAddress.split("\\.");
        if (finished) {
            clientPort = split[0] + "." + split[1];
        } else {
            clientPort = NOT_FINISHED;
        }
        int rank = findRankOfImage(renderedImageIndex);
        clientPortTab[rank] = clientPort;
        repaint();
    }

    private int findRankOfImage(int imageNumber) {
        int rank = 0;
        while (rank <= imageIndexTab.length && imageIndexTab[rank] != imageNumber) {
            rank++;
        }
        if (rank == imageIndexTab.length) {
            return 0;
        } else {
            return rank;
        }
    }

    private void paintOneSquare(int imageIndex, int squareWidth, Graphics g) {
        int line = imageIndex / nbColumns;
        int col = imageIndex - line * nbColumns;
        int x = col * squareWidth;
        int y = line * squareWidth;
        String client = clientPortTab[imageIndex];
        Color color = chooseColor(client);
        g.setColor(color);
        g.fillRect(x, y, squareWidth, squareWidth);
        // Paint image index, which is either the actual number of the image (not necessarily starting at zero), or the rank in the array.
        g.setColor(Color.gray);
        int paintedIndex = (useArray ? imageIndex : imageIndex + imageIndexTab[0]);
        g.drawString(paintedIndex + "", x + 2, y - 1 + squareWidth);
        // Paint the client id if the image is done
        if (!client.equals(NOT_STARTED) && !client.equals(NOT_FINISHED)) {
            g.drawString(client + "", x + 2, y - 1 + squareWidth / 2);
        }

        // Paint the border of the square
        g.setColor(Color.gray);
        g.drawRect(x, y, squareWidth, squareWidth);
    }

    @Override
    public void paintComponent(Graphics g) {

        // Erase everything
        g.setColor(Color.white);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        int squareWidth = this.getWidth() / nbColumns;

        // Paint the squares.
        for (int imageIndex = 0; imageIndex < clientPortTab.length; imageIndex++) {
            paintOneSquare(imageIndex, squareWidth, g);
        }
    }

    private Color chooseColor(String client) {

        // The color used for this client
        Color color;

        if (client.equals(NOT_STARTED)) {
            // Image not allocated.
            color = Color.black;
        } else if (client.equals(NOT_FINISHED)) {
            // Image being computed
            color = Color.gray;
        } else if (!colors.containsKey(client + "")) {
            // A new client rendered its first image
            color = availableColors.remove(0);
            colors.put(client + "", color);
        } else {
            color = colors.get(client + "");
        }

        return color;
    }

    /**
     * Set the height to the appropriate value, given the current width of the
     * panel.
     *
     */
    private void computeHeight() {

        if (nbLines == nbColumns) {
            newHeight = this.getWidth();
        } else {
            int squareSize = this.getWidth() / nbColumns;
            newHeight = this.getWidth() + squareSize;
        }

    }

    public void increaseNbCols(boolean mustIncrease) {
        if (mustIncrease) {
            nbColumns++;
        } else {
            if (nbColumns > 1) {
                nbColumns--;
            }
        }
        nbLines = clientPortTab.length / nbColumns;
        repaint();
    }

    /**
     * When doing a sequence, set all the indices with a step of 1.
     *
     * @param firstImageIndex
     */
    public void setFirstImageIndex(int firstImageIndex) {
        for (int i = 0; i < imageIndexTab.length; i++) {
            imageIndexTab[i] = firstImageIndex + i;
        }
    }

    /**
     * When doing a list of images (non-sequential), set the indices.
     *
     */
    public void setAllIndices(int[] array) {
        imageIndexTab = array;
    }
}
