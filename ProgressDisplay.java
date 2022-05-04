package blenderparallelrendering;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

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

    private static final String NOT_STARTED = "NOT_STARTED";
    private static final String NOT_FINISHED = "NOT_FINISHED";
    private static final String PROBABLY = "probably";

    private int displayOffset = 0;

    private int textfieldWidth = 5;

    private JPanel mainPanel;

    private JPanel jobsPanel;
    private JLabel jobsPanelTitle;
    private CustomJTable jobsTable;

    private JPanel jobDetailsPanel;
    private JLabel jobDetailsTitle;
    private CustomJTable jobDetailsTable;

    private JTable hostTable;

    private JPanel toolbar;

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String CANCEL = "cancel";

    private ArrayList<Subscriber> listeners;

    private GridBagConstraints c;

    public ProgressDisplay() {

        listeners = new ArrayList<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new GridLayout());
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        jobsPanel = new JPanel();
        jobsPanel.setLayout(new BorderLayout());

        jobsPanel.setBackground(Color.gray);

        requestDetails(-1);

        jobDetailsTitle = new JLabel("Selected Job");
        jobDetailsPanel = new JPanel();

        jobDetailsPanel.setLayout(new BorderLayout());
        jobDetailsPanel.add(jobDetailsTitle, BorderLayout.NORTH);
        jobDetailsPanel.add(jobDetailsTable, BorderLayout.CENTER);
        jobDetailsPanel.setBackground(Color.gray.brighter());

        jobsPanelTitle = new JLabel("Jobs Panel Title");

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.ipadx = 10;
        c.ipady = 10;
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(jobsPanelTitle, c);
        c.gridy = 2;
        mainPanel.add(jobDetailsTitle, c);
        c.gridy = 3;
        mainPanel.add(jobDetailsPanel, c);

        add(mainPanel);

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
     * Send to the server a request for detailed information about a given job.
     *
     * @param row the index of the selected job in the jobTable. If -1, then
     * generate placeholders
     */
    private void requestDetails(int row) {
        String jobID;
        if (row == -1) {
            jobID = "0";
        } else {
            jobID = jobsTable.getValueAt(row, 0) + "";
        }
        String notification = "DETAILS " + jobID;
        notifyListeners(notification);
        Object dataDetails[][] = new Object[][]{
            {"-", "-", "-"}
        };

        String[] detailsColumn = new String[]{"Frame", "Status", "Host"};

        jobDetailsTable = new CustomJTable(dataDetails, detailsColumn);
    }

    /**
     * Tell the server to change the status of the task.
     *
     */
    private void actionChangeRequest(int taskNumber) {
        String command = (String) jobsTable.getModel().getValueAt(taskNumber, jobsTable.getColumnCount() - 2);
        String jobName = (String) jobsTable.getModel().getValueAt(taskNumber, 0);
        String notification = command + " " + jobName;
        notifyListeners(notification);
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
                createNewJob(singleTaskPanel);
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
    private void createNewJob(JPanel singleTaskPanel) {

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
        if (!listeners.contains(s)) {
            listeners.add(s);
        }
    }

    @Override
    public void update(String message) {

        String[] words = message.split(" ");
        if (words[0].equals("NEW_JOB")) {
            // Add a new job to the list.
            addNewJob(words);
        } else if (words[0].equals("JOB_STARTED")) {
            int jobID = Integer.valueOf(words[1]);
            flagJobAsStarted(jobID, true);
        } else if (words[0].equals("JOB_STOPPED")) {
            int jobID = Integer.valueOf(words[1]);
            flagJobAsStarted(jobID, false);
        }
    }

    /**
     * Flag a job as started or stopped, depending on the value of the boolean.
     *
     * @param jobID
     * @param started true for a job that just started, false for a job that
     * stopped.
     */
    private void flagJobAsStarted(int jobID, boolean started) {
        DefaultTableModel model = (DefaultTableModel) jobsTable.getModel();
        if (started) {
            model.setValueAt("In progress", jobID, 1);
            model.setValueAt("Stop", jobID, 5);
        } else {
            model.setValueAt("Waiting", jobID, 1);
            model.setValueAt("Start", jobID, 5);
        }
        repaint();
    }

    private void notifyListeners(String string) {
        for (Subscriber s : listeners) {
            s.update(string);
        }
    }

    private void addNewJob(String[] words) {

        String jobName = words[1];
        String status = "Waiting";
        String firstImage = words[2];
        String lastImage = words[3];

        if (jobsTable == null) {
            // Create the table

            String[] jobsTableColumns = new String[]{
                "Job name", "status", "First Image", "Last image",
                "Done/Total", "Action", "Details"};
            Object dataTest[][] = new Object[][]{
                {jobName, "status", firstImage, lastImage,
                    "Done/Total", "Action", "Details"}
            };
            jobsTable = new CustomJTable(dataTest, jobsTableColumns);
            jobsPanel.add(jobsTable.getTableHeader(), BorderLayout.NORTH);
            jobsPanel.add(jobsTable, BorderLayout.CENTER);

            jobsTable.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int row = jobsTable.rowAtPoint(e.getPoint());
                    int col = jobsTable.columnAtPoint(e.getPoint());

                    String header = jobsTable.getHeader(col);
                    if (header.equals("Action")) {
                        // Tell the server to do stuff
                        actionChangeRequest(row);
                    } else if (header.equals("Details")) {
                        // Change the data in the 'Job Detail List' table.
                        requestDetails(row);
                    }
                }
            }
            );

            c.gridy = 1;
            mainPanel.add(jobsPanel, c);
        }

        jobsPanel.add(jobsTable, BorderLayout.CENTER);

        // Simply add the new data
        DefaultTableModel model = (DefaultTableModel) jobsTable.getModel();
        String[] newRow = {jobName, status, firstImage, lastImage, "15/100", "Start", "Details"};
        model.addRow(newRow);
    }
}
