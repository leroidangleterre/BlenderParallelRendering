/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blenderparallelrendering;

import java.awt.Color;
import java.awt.Graphics;
import javax.swing.JPanel;

/**
 *
 * @author arthurmanoha
 */
public class ProgressDisplay extends JPanel {

    // This tab represents the distribution of the clients that rendered the images.
    // It contains the id of the client, or -1 if the image was not rendered yet.
    int[] clientPortTab;
    int firstImageIndex; // The index of the image represented by the first slot in indexTab[]

    int nbLines, nbColumns;
    int newHeight;

    public ProgressDisplay(int nbImages, int firstImageIndexParam) {

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

//        this.addComponentListener(new DisplayListener());
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
    public void update(int renderedImageIndex, int clientPort) {

        // The image 'renderedImageIndex' is represented by the slot 'renderedImageIndex - firstImageIndex'
        // in indexTab;
        int index = renderedImageIndex - firstImageIndex - 1;
        clientPortTab[index] = clientPort;
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
//        System.out.println("paintComponent");
//        System.out.println(nbLines + " lines, " + nbColumns + " columns.");

        int squareWidth = this.getWidth() / nbColumns;
//        System.out.println("each square is " + squareWidth + " pixels wide.");

        // Paint the squares.
        for (int imageIndex = 0; imageIndex < clientPortTab.length; imageIndex++) {
            int line = imageIndex / nbColumns;
            int col = imageIndex - line * nbColumns;
            int x = col * squareWidth;
            int y = line * squareWidth;
            int client = clientPortTab[imageIndex];
            Color color = chooseColor(client);
            g.setColor(color);
            g.fillRect(x, y, squareWidth, squareWidth);
        }

        g.setColor(Color.gray);
        // Paint the limits between the columns.
        for (int col = 0; col < nbColumns; col++) {
            int x = col * squareWidth;
            g.drawLine(x, 0, x, nbLines * squareWidth);
        }
        // Paint the limits between the lines.
        for (int line = 0; line < nbLines; line++) {
            int y = line * squareWidth;
            g.drawLine(0, y, nbColumns * squareWidth, y);
        }
    }

    private Color chooseColor(int client) {
        Color color;
        if (client == -1) {
            color = Color.black;
        } else {
            switch (client) {
            case 88:
                color = Color.blue;
                break;
            case 55:
                color = Color.orange;
                break;
            case 50:
                color = Color.red;
                break;
            case 43:
                color = Color.yellow;
                break;
            default:
                color = Color.white;
                break;
            }

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

//    private class DisplayListener implements ComponentListener {
//
//        public DisplayListener() {
//        }
//
//        @Override
//        public void componentResized(ComponentEvent e) {
//            computeHeight();
//            System.out.println("Component resized to " + newHeight);
//            Dimension newSize = new Dimension(getWidth(), newHeight);
//            setPreferredSize(newSize);
//            setSize(newSize);
//            setMinimumSize(newSize);
//        }
//
//        @Override
//        public void componentMoved(ComponentEvent e) {
//        }
//
//        @Override
//        public void componentShown(ComponentEvent e) {
//        }
//
//        @Override
//        public void componentHidden(ComponentEvent e) {
//        }
//    }
}
