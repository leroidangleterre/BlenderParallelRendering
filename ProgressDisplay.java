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
    String[] clientAddressTab;
    int[] imageIndexTab;
    IMAGE_STATUS[] imageStatus; // NOT_STARTED, IN_PROGESS, DONE.
    enum IMAGE_STATUS {
        NOT_STARTED, IN_PROGRESS, DONE
    };

    private final HashMap<String, Color> colors;
    private final ArrayList<Color> availableColors;

    int nbLines, nbColumns;
    int newHeight;

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

        clientAddressTab = new String[array.length];
        imageIndexTab = new int[array.length];
        imageStatus = new IMAGE_STATUS[array.length];

        for (int i = 0; i < array.length; i++) {
            clientAddressTab[i] = "no_address";
            imageIndexTab[i] = array[i];
            imageStatus[i] = IMAGE_STATUS.NOT_STARTED;
        }

        nbColumns = 20;
        nbLines = clientAddressTab.length / nbColumns;
        if (nbColumns * nbLines != clientAddressTab.length) {
            // Not a perfect square, need to add a line.
            nbLines++;
        }

        computeHeight();

        this.addMouseWheelListener(this);

        repaint();
    }

    /**
     * Tell the progress display that the n-th image has been rendered or is
     * being rendered. When the client already has an unfinished frame, it means
     * that it just reconnected and that the previously assigned frame is lost.
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

        if (renderedImageIndex != -1) { // If computation is still ongoing

            int rank = findRankOfImage(renderedImageIndex);
            clientAddressTab[rank] = clientAddress;
            if (finished) {
                imageStatus[rank] = IMAGE_STATUS.DONE;
            } else {
                imageStatus[rank] = IMAGE_STATUS.IN_PROGRESS;
                cancelUnfinishedFrameByClient(clientAddress, renderedImageIndex);
            }
            repaint();
        }
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

    private void paintOneSquare(int squareIndex, int frameNumber, int squareWidth, int remainingPixels, Graphics g) {
        int line = squareIndex / nbColumns;
        int col = squareIndex - line * nbColumns;
        int x = col * squareWidth + (remainingPixels * col) / nbColumns;
        int y = (line + displayOffset) * squareWidth;
        String client;

        if (frameNumber == -1) {
            client = "no_client_yet";
        } else {
            client = clientAddressTab[squareIndex];
        }

        Color color = chooseColor(client);
        g.setColor(color);

        switch (imageStatus[squareIndex]) {
        case IN_PROGRESS:
            // paint half square with the client's color
            int xTab[] = {x, x + squareWidth, x};
            int yTab[] = {y, y, y + squareWidth};
            g.fillPolygon(xTab, yTab, 3);
            // Paint the rest gray
            xTab[0] = x + squareWidth;
            yTab[0] = y + squareWidth;
            g.setColor(Color.gray.darker());
            g.fillPolygon(xTab, yTab, 3);
            break;
        case NOT_STARTED:
            g.setColor(Color.gray.darker());
            g.fillRect(x, y, squareWidth, squareWidth);
            break;
        case DONE:
            // paint full square with the client's color
            g.setColor(color);
            g.fillRect(x, y, squareWidth, squareWidth);
            break;
        }

        // Paint image index
        g.setColor(Color.gray);
        g.drawString(frameNumber + "", x + 2, y - 1 + squareWidth);
        g.drawString(client + "", x + 2, y - 1 + squareWidth / 2);

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
        for (int i = 0; i < clientAddressTab.length; i++) {
            // The squares are numbered from zero, but the images may have different indices.
            int frameNumber = imageIndexTab[i]; // Frame number in Blender
            try {
                paintOneSquare(i, frameNumber, squareWidth, remainingPixels, g);
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Exception for square index " + i + ", image index: " + i);
            }
        }
    }

    private Color chooseColor(String client) {

        // The color used for this client
        Color color;

        if (imageStatus.equals(IMAGE_STATUS.NOT_STARTED)) {
            // Image not allocated.
            color = Color.black;
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
     * Set the height to the appropriate value, given the current width of
     * the panel.
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
        nbLines = clientAddressTab.length / nbColumns;
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
            clientAddressTab[index] = clientAddressTab[index + 1];
            imageStatus[index] = imageStatus[index + 1];
        }
        // Place the chosen image at the end.
        imageIndexTab[imageIndexTab.length - 1] = imageToReset;
        clientAddressTab[clientAddressTab.length - 1] = "no_address_again";
        imageStatus[clientAddressTab.length - 1] = IMAGE_STATUS.NOT_STARTED;
        repaint();
    }

    /**
     * Look for an image that is being rendered by this client; if such an image
     * exists, invalidate it.
     *
     * @param clientAddress
     * @param renderedImageIndex The image that is in progress; any other
     * in-progress image marked with the same client must be cancelled.
     */
    private void cancelUnfinishedFrameByClient(String clientAddress, int renderedImageIndex) {

        for (int rank = 0; rank < imageIndexTab.length; rank++) {

            if (clientAddressTab[rank].equals(clientAddress)) {
                // This image in the list is the responsability of this client.
                switch (imageStatus[rank]) {
                case IN_PROGRESS:
                    // If that image is not the one being rendered now, then it was cancelled by the client.
                    // It must be officially invalidated.
                    if (imageIndexTab[rank] != renderedImageIndex) {
                        invalidateImage(imageIndexTab[rank]);
                    }
                    break;
                case NOT_STARTED:
                    // Don't do anything, image not assigned yet.
                    break;
                case DONE:
                    break;
                }
            }
        }
    }
}
