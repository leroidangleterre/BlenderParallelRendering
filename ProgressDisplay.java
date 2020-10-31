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
    int[] clientPortTab;
    private HashMap<String, Color> colors;
    private ArrayList<Color> availableColors;
    int firstImageIndex; // The index of the image represented by the first slot in indexTab[]

    int nbLines, nbColumns;
    int newHeight;

    int currentIndex;
    boolean useArray;

    public ProgressDisplay(int nbImages, int firstImageIndexParam, boolean useArrayParam) {

        // Hashmap key: string value of client; value: allocated color.
        colors = new HashMap<>();
        availableColors = new ArrayList<>();
        availableColors.add(Color.red);
        availableColors.add(Color.blue);
        availableColors.add(Color.yellow);
        availableColors.add(Color.green);
        availableColors.add(Color.cyan);

        clientPortTab = new int[nbImages];
        for (int i = 0; i < nbImages; i++) {
            clientPortTab[i] = -1;
        }

        firstImageIndex = firstImageIndexParam;

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
     * @param clientPort the id of the client that rendered said image.
     */
    public void update(int renderedImageIndex, int clientPort, boolean useArray) {

        // The image 'renderedImageIndex' is represented by the slot 'renderedImageIndex - firstImageIndex'
        // in indexTab;
        int index;
        if (useArray) {
            index = renderedImageIndex;
        } else {
            index = renderedImageIndex - firstImageIndex - 1;
        }
        clientPortTab[index] = clientPort;
        repaint();
    }

    public void update(int renderedImageIndex, int clientPort) {
        update(renderedImageIndex, clientPort, false);
    }

    private void paintOneSquare(int imageIndex, int squareWidth, Graphics g) {
        int line = imageIndex / nbColumns;
        int col = imageIndex - line * nbColumns;
        int x = col * squareWidth;
        int y = line * squareWidth;
        int client = clientPortTab[imageIndex];
        Color color = chooseColor(client);
        g.setColor(color);
        g.fillRect(x, y, squareWidth, squareWidth);
        // Paint image index, which is either the actual number of the image (not necessarily starting at zero), or the rank in the array.
        g.setColor(Color.gray);
        int paintedIndex = (useArray ? imageIndex : imageIndex + firstImageIndex);
        g.drawString(paintedIndex + "", x + 2, y - 1 + squareWidth);
        // Paint the client id
        if (client != -1) {
            g.drawString(client + "", x + 2, y - 1 + squareWidth / 2);
        }

        // Paint the borderr of the square
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

    private Color chooseColor(int client) {

        // The color used for this client
        Color color;

        if (client != -1 && !colors.containsKey(client + "")) {
            // Choose a new color for this new client. That color will no longer be available for other clients.
            Color newColor = availableColors.remove(0);
            colors.put(client + "", newColor);
        }

        if (client == -1) {
            color = Color.black;
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

    public void setFirstImageIndex(int newIndex) {
        firstImageIndex = newIndex;
    }
}
