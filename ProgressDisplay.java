/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package blenderparallelrendering;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author arthurmanoha
 */
public class ProgressDisplay extends JFrame implements MouseWheelListener, Subscriber {

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

    int textfieldWidth = 5;

    private ArrayList<Subscriber> subList;

    public ProgressDisplay() {

        subList = new ArrayList<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new BorderLayout());
        JPanel topButtonsPanel = new JPanel();
        topButtonsPanel.setLayout(new BorderLayout());
        JPanel allTasksPanel = new JPanel();
        allTasksPanel.setLayout(new BoxLayout(allTasksPanel, BoxLayout.PAGE_AXIS));
        JPanel bottomButtonsPanel = new JPanel();
        bottomButtonsPanel.setBackground(Color.gray);
        setButtons(topButtonsPanel, allTasksPanel, bottomButtonsPanel);

        add(topButtonsPanel, BorderLayout.NORTH);
        add(allTasksPanel, BorderLayout.CENTER);
        add(bottomButtonsPanel, BorderLayout.SOUTH);
        setVisible(true);

        // Hashmap key: string value of client; value: allocated color.
        colors = new HashMap<>();
        availableColors = new ArrayList<>();
        availableColors.add(Color.red);
        availableColors.add(Color.blue);
        availableColors.add(Color.yellow);
        availableColors.add(Color.green);
        availableColors.add(Color.cyan);

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
    public void update(String renderedImageInfo, String clientAddress, boolean finished) {

        String clientPort;

        if (finished) {
            clientPort = clientAddress.substring(0, 5);
            // remember clientPort as the latest client
            latestClient = clientPort;
        } else {
            // This is probably the client who rendered the latest image before this one.
            clientPort = latestClient + PROBABLY;
        }
        int rank = findRankOfImage(renderedImageInfo);
        clientPortTab[rank] = clientPort;
        repaint();
    }

    protected int findRankOfImage(String imageInfo) {
        int rank = 0;
        // TODO
        return rank;
    }

    private void setButtons(JPanel topPanel, JPanel allTasksPanel, JPanel bottomPanel) {

        JButton addTaskButton = new JButton("Add task");
        addTaskButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPanel singleTaskPanel = new JPanel();
                createNewTask(singleTaskPanel);
                System.out.println("Task created.");
                singleTaskPanel.setBackground(getRandomColor());
                allTasksPanel.add(singleTaskPanel);
                revalidate();
            }

            private Color getRandomColor() {
                int val = (int) (Math.random() * 256);
                return new Color(val, val, val);
            }

        });

        bottomPanel.add(addTaskButton);
    }

    /**
     * Add a task to the progress display, and set it up for the server.
     *
     * @param p
     */
    private void createNewTask(JPanel singleTaskPanel) {

        JTextArea jobId = new JTextArea("id 0");

        JTextField filenameField = new JTextField("", 20);
        JButton chooseFileButton = new JButton("select file");
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File("D:\\Blender"));

        JTextField startIndex = new JTextField("", textfieldWidth);
        startIndex.setToolTipText("start frame");

        JTextField endIndex = new JTextField("", textfieldWidth);
        endIndex.setToolTipText("end frame");

        JButton goOrStopButton = new JButton("Go");
        goOrStopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                String infoText;
                if (fileChooser.getSelectedFile() != null && !startIndex.getText().equals("") && !endIndex.getText().equals("")) {
                    infoText = fileChooser.getSelectedFile().getName() + " " + startIndex.getText() + " " + endIndex.getText();
                    if (goOrStopButton.getText().equals("Go")) {
                        // Need to start the task
                        infoText = "Go " + infoText;
                    } else {
                        // Need to stop it.
                        infoText = "Stop " + infoText;
                    }
                    notifyListeners(infoText);
                } else {
                    System.out.println("Cannot tell server: missing info in task description.");
                }
            }
        }
        );
        JTextField progressIndicator = new JTextField("0/n");
        JButton removeButton = new JButton("Remove task");

        chooseFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int result = fileChooser.showOpenDialog(singleTaskPanel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    filenameField.setText(fileChooser.getSelectedFile().getName());
                }
                revalidate();
            }
        });

        singleTaskPanel.add(jobId);
        singleTaskPanel.add(filenameField);
        singleTaskPanel.add(chooseFileButton);
        singleTaskPanel.add(startIndex);
        singleTaskPanel.add(endIndex);
        singleTaskPanel.add(goOrStopButton);
        singleTaskPanel.add(progressIndicator);
        singleTaskPanel.add(removeButton);
    }

    protected void invalidateImage(int imageToReset) {
        System.out.println("invalidateImage TODO");
        // TODO
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    // Add a new listener that will be informed about the tasks.
    public void addListener(Subscriber s) {
        if (!subList.contains(s)) {
            subList.add(s);
        }
    }

    public void update(String message) {
        System.out.println("Progress display reseives message: " + message);
    }

    private void notifyListeners(String string) {
        for (Subscriber s : subList) {
            s.update(string);
        }
    }
}
