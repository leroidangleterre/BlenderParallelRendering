package blenderparallelrendering;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author arthurmanoha
 */
public class ServerDisplay extends JFrame implements MouseWheelListener, Subscriber {

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
    private JScrollPane jobDetailScrollPane;
    private CustomJTable jobDetailsTable;
    private int currentlyDisplayedJobID;

    private JTable hostTable;

    private JPanel toolbar;

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final String CANCEL = "cancel";

    private ArrayList<Subscriber> listeners;

    private GridBagConstraints c;

    public ServerDisplay() {

        listeners = new ArrayList<>();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setLayout(new GridLayout());
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());

        jobsPanel = new JPanel();
        jobsPanel.setLayout(new BorderLayout());

        jobsPanel.setBackground(Color.gray);

        requestDetails(-1);
        jobDetailsPanel = new JPanel();
        jobDetailsPanel.setLayout(new BorderLayout());
        currentlyDisplayedJobID = -1;

        jobDetailsTitle = new JLabel("Selected job");
        jobsPanelTitle = new JLabel("All jobs");

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.ipadx = 10;
        c.ipady = 10;
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(jobsPanelTitle, c);
        c.gridy = 2;
        mainPanel.add(jobDetailsTitle, c);

        // Scroll pane that contains jobDetailsPanel
        jobDetailScrollPane = new JScrollPane(jobDetailsPanel);
        jobDetailScrollPane.setPreferredSize(new Dimension(500, 500));
        jobDetailScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jobDetailScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        c.gridy = 3;
        mainPanel.add(jobDetailScrollPane, c);

        add(mainPanel);

        setTitle("Server window");
        setVisible(true);

        // Hashmap key: string value of client; value: allocated color.
        colors = new HashMap<>();
        availableColors = new ArrayList<>();
        availableColors.add(Color.red);
        availableColors.add(Color.blue);
        availableColors.add(Color.yellow);
        availableColors.add(Color.green);
        availableColors.add(Color.cyan);
        revalidate();
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
            jobID = "-1";
        } else {
            jobID = jobsTable.getValueAt(row, 0) + "";
        }
        String notification = "DETAILS " + jobID;
        notifyListeners(notification);
    }

    /**
     * Tell the server to change the status of the task.
     *
     */
    private void actionChangeRequest(int taskNumber) {
        if (taskNumber != -1) {
            String command = (String) jobsTable.getModel().getValueAt(taskNumber, jobsTable.getColumnCount() - 2);
            String jobName = (String) jobsTable.getModel().getValueAt(taskNumber, 0);
            String notification = command + " " + jobName;
            notifyListeners(notification);
        }
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
        int jobID;

        switch (words[0]) {
        case "NEW_JOB":
            // Add a new job to the list.
            addNewJob(words);
            break;
        case "JOB_STARTED":
            jobID = Integer.valueOf(words[1]);
            flagJobAsStarted(jobID, true);
            break;
        case "JOB_STOPPED":
            jobID = Integer.valueOf(words[1]);
            flagJobAsStarted(jobID, false);
            break;
        case "FRAME_ASSIGNED":
            // Only update if the table is currently displaying this job
            String jobTitle = words[1];
            if (jobTitle.equals(jobDetailsTitle.getText())) {
                // The updated frame is part of the currently displayed job, so we update the table.
                jobID = Integer.valueOf(words[3]);
                try {
                    int frame = Integer.valueOf(words[2]);
                    flagImageAsStarted(jobID, frame, true);
                } catch (NumberFormatException ex) {
                    System.out.println("ServerDisplay.update(" + message + "): " + ex);
                }
            }
            break;
        case "JOB_DETAILS":
            // Rebuild jobDetailsTable with the new info
            buildDetailsTable(words);
            break;
        case "FILENAME_CHANGED":
            // Transmit the info to the server
            notifyListeners(message);
            break;
        case "SET_FIRST_FRAME":
            notifyListeners(message);
            break;
        case "SET_LAST_FRAME":
            notifyListeners(message);
            break;
        default:
            break;
        }

        jobDetailsPanel.revalidate();
        revalidate();
        repaint();
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

    /**
     * Flag an image as started or stopped. Only useful when the job's details
     * are being displayed.
     *
     * @param jobID
     * @param frame
     * @param b
     */
    private void flagImageAsStarted(int jobID, int frame, boolean started) {

        if (jobDetailsTable != null) {
            DefaultTableModel model = (DefaultTableModel) jobDetailsTable.getModel();
            if (started) {
                model.setValueAt("In progress", frame, 1);
            }
        }
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

            jobsTable.setColumnWidth();

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
            });
            jobsTable.addListener(this);

            c.gridy = 1;
            mainPanel.add(jobsPanel, c);
        }

        jobsPanel.add(jobsTable, BorderLayout.CENTER);

        // Simply add the new data
        DefaultTableModel model = (DefaultTableModel) jobsTable.getModel();
        String[] newRow = {jobName, status, firstImage, lastImage, "15/100", "Start", "Details"};
        model.addRow(newRow);
    }

    // Add all the information about frames to the GUI table.
    private void buildDetailsTable(String[] jobInfo) {

        Object dataDetails[][] = new Object[][]{
            {"-", "-", "-"}
        };

        String[] detailsColumn = new String[]{"Frame", "Status", "Host"};

        // Create the table, or clear the previous values.
        if (jobDetailsTable == null) {
            jobDetailsTable = new CustomJTable(dataDetails, detailsColumn);
            jobDetailsPanel.add(jobDetailsTable);
            System.out.println("Table created.");
        } else {
            // Clear table
            while (jobDetailsTable.getRowCount() > 0) {
                ((DefaultTableModel) jobDetailsTable.getModel()).removeRow(0);
            }
            System.out.println("Table cleared.");
        }

        for (String infoLine : jobInfo) {
            System.out.println("    info line <" + infoLine + ">");
            String info[] = infoLine.split(":");

            if (infoLine.contains("FRAME")) {
                // Image details, format "FRAME:4:NOT_STARTED:none"
                String frameNumber = info[1];
                String status = info[2];
                String host = info[3];

                // Simply add the new data
                DefaultTableModel model = (DefaultTableModel) jobDetailsTable.getModel();
                String[] newRow = {frameNumber, status, host};
                model.addRow(newRow);
            } else if (infoLine.contains("FILENAME")) {
                // filename
                jobDetailsTitle.setText(info[1]);
            } else if (infoLine.contains("date")) {
                // file date
            }
        }
    }
}
