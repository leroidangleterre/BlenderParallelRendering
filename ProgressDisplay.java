/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blenderparallelrendering;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JPanel;

/**
 *
 * @author arthurmanoha
 */
public class ProgressDisplay extends JPanel implements MouseWheelListener {

    // This tab represents the distribution of the clients that rendered the images.
    // It contains the id of the client, or -1 if the image was not rendered yet.
    String[] clientPortTab;
    String latestClient;
    int[] imageIndexTab;
    private final HashMap<String, Color> colors;
    private final ArrayList<Color> availableColors;

    int nbLines, nbColumns;
    int newHeight;

    private static final String NOT_STARTED = "NOT_STARTED";
    private static final String NOT_FINISHED = "NOT_FINISHED";
    private static final String PROBABLY = "probably";

    private int displayOffset = 0;

    // A ProgressDisplay is always created with a complete list of indices.
    public ProgressDisplay(int[] array) {
        // Hashmap key: string value of client; value: allocated color.
        colors = new HashMap<>();
        availableColors = new ArrayList<>();
        availableColors.add(Color.red);
        availableColors.add(Color.blue);
        availableColors.add(Color.yellow);
        availableColors.add(Color.green);
        availableColors.add(Color.cyan);

        clientPortTab = new String[array.length];
        imageIndexTab = new int[array.length];

        for (int i = 0; i < array.length; i++) {
            clientPortTab[i] = NOT_STARTED;
            imageIndexTab[i] = array[i];
        }

        nbColumns = 20;
        nbLines = clientPortTab.length / nbColumns;
        if (nbColumns * nbLines != clientPortTab.length) {
            // Not a perfect square, need to add a line.
            nbLines++;
        }

        computeHeight();

        this.addMouseWheelListener(this);

        repaint();
    }

    /**
     * Tell the progress display that the n-th image has been rendered.
     *
     * @param renderedImageIndex the index of the image, starting from ZERO
     * (i.e. if we render images 3255 through 3265, then values range from 0 to
     * 10);
     * @param clientAddress the id of the client that rendered said image;
     * format: 0x123456...
     * @param finished true when the image is confirmed done, false when it is
     * still being computed.
     */
    public void update(int renderedImageIndex, String clientAddress, boolean finished) {

        String clientPort;

        if (finished) {
            clientPort = clientAddress.substring(0, 5);
            // remember clientPort as the latest client
            latestClient = clientPort;
        } else {
            // This is probably the client who rendered the latest image before this one.
            clientPort = latestClient + PROBABLY;
        }
        int rank = findRankOfImage(renderedImageIndex);
        clientPortTab[rank] = clientPort;
        repaint();
    }

    protected int findRankOfImage(int imageNumber) {
        int rank = 0;
        while (rank < imageIndexTab.length && imageIndexTab[rank] != imageNumber) {
            rank++;
        }
        if (rank == imageIndexTab.length) {
            return 0;
        } else {
            return rank;
        }
    }

    private void paintOneSquare(int squareIndex, int imageIndex, int squareWidth, int remainingPixels, Graphics g) {
//        System.out.println("paintOneSquare; squareIndex: " + squareIndex + ", image index: " + imageIndex);
        int line = squareIndex / nbColumns;
        int col = squareIndex - line * nbColumns;
        int x = col * squareWidth + (remainingPixels * col) / nbColumns;
        int y = (line + displayOffset) * squareWidth;
        String client;

        if (imageIndex == -1) {
            client = "no_client_yet";
        } else {
            client = clientPortTab[squareIndex];
        }

//        Color color = chooseColor(NOT_STARTED);
        Color color = chooseColor(client);
        g.setColor(color);

        if (client.contains(PROBABLY)) {
            // paint half square with the client's color
            int xTab[] = {x, x + squareWidth, x};
            int yTab[] = {y, y, y + squareWidth};
            g.fillPolygon(xTab, yTab, 3);
            // Paint the rest black
            xTab[0] = x + squareWidth;
            yTab[0] = y + squareWidth;
            g.setColor(Color.black);
            g.fillPolygon(xTab, yTab, 3);
        } else {
            // paint full square
            g.fillRect(x, y, squareWidth, squareWidth);
        }

        // Paint image index
        g.setColor(Color.gray);
        g.drawString(imageIndex + "", x + 2, y - 1 + squareWidth);
        // Paint the client id if the image is done
        if (!client.equals(NOT_STARTED) && !client.equals(NOT_FINISHED)) {
            if (client.contains(PROBABLY)) {
                client = client.substring(0, client.length() - PROBABLY.length());
            }
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

        // The additional pixels will be spread among the columns.
        int remainingPixels = this.getWidth() - squareWidth * nbColumns;

        // Paint the squares.
        for (int squareIndex = 0; squareIndex < clientPortTab.length; squareIndex++) {
            // The squares are numbered from zero, but the images may have different indices.
            int imageIndex = imageIndexTab[squareIndex];
            try {
                paintOneSquare(squareIndex, imageIndex, squareWidth, remainingPixels, g);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Exception for square index " + squareIndex + ", image index: " + imageIndex);
            }
        }
    }

    private Color chooseColor(String client) {

        // The color used for this client
        Color color;

        if (client.equals(NOT_STARTED)) {
            // Image not allocated.
            color = Color.black;
        } else if (client.contains(PROBABLY)) {
            // Not exactly sure about which client it is, but it's very likely.
            String probableClient = client.substring(0, client.length() - PROBABLY.length());
            color = colors.get(probableClient);
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
     * @param array the indices of the images we need to render
     */
    public void setAllIndices(int[] array) {
        imageIndexTab = array;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
            nbColumns += e.getWheelRotation();
        } else {
            displayOffset -= e.getWheelRotation();
        }
        repaint();
    }

    protected void invalidateImage(int imageToReset) {

        int rank = findRankOfImage(imageToReset);
        // Shift one step to the left all the images that were after the reset image.
        for (int index = rank; index < imageIndexTab.length - 1; index++) {
            imageIndexTab[index] = imageIndexTab[index + 1];
            clientPortTab[index] = clientPortTab[index + 1];
        }
        // Place the chosen image at the end.
        imageIndexTab[imageIndexTab.length - 1] = imageToReset;
        clientPortTab[clientPortTab.length - 1] = NOT_STARTED;
        repaint();
    }
}
