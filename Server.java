package blenderparallelrendering;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

/**
 *
 * @author arthu
 */
public class Server implements Subscriber {

    private long startDate;
    private ArrayList<Job> jobList;
    private int IMAGE_INDEX;

    private ServerDisplay display;
    private AverageCalculator avgCalc;
    private ConnectionHandler cnxHandler;
    public int nbClientsConnected = 0;
    public int NODE_NUMBER = 0;
    private String targetDirectory;

    private ArrayList<Subscriber> listeners;

    public Server() {
        jobList = new ArrayList<>();
        listeners = new ArrayList<>();
    }

    public void run() {
        int port = 65432;
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            InetAddress localhost = InetAddress.getLocalHost();
            System.out.println("address: " + localhost.getHostAddress());

            startDate = System.currentTimeMillis();

            IMAGE_INDEX = 0;

            avgCalc = new AverageCalculator();
            while (true) {
                Socket clientSocket = serverSocket.accept();
                cnxHandler = new ConnectionHandler(clientSocket, display);
                new Thread(cnxHandler).start();
            }

        } catch (IOException e) {
            System.out.println("Server: IOException");
            System.out.println(e);
        }
    }

    /**
     * Increment the index and return the new value. Synchronization makes sure
     * no image index is sent twice.
     *
     * @return the image index that the thread must render.
     */
    public synchronized String getNextImageInfo() {

        String info;
        int jobRank = 0;

        for (Job job : jobList) {
            info = job.getNextImageInfo();
            if (!info.equals("none")) {
                System.out.println("Server requested info " + info + " from Job ranked " + jobRank);
                // Tell the display that an image is being assigned to a client.
                String notif = "FRAME_ASSIGNED " + info + " " + jobRank;
                notifyListeners(notif);
                return info;
            }
            jobRank++;
        }
        return "none";
    }

    /**
     * The image with the given index must be removed from the list, marked 'not
     * rendered yet', and put back at the end of the list.
     *
     * @param imageToReset the index of the image that is being reset
     */
    private void invalidateImage(int imageToReset) {
        // TODO
    }

    void setTargetDirectory(String targetDir) {
        targetDirectory = targetDir;
    }

    /**
     * Create a single job, as a user would do, for testing purposes.
     */
    public void createTestJob() {
        Job j;
        switch (jobList.size()) {
        case 0:
            j = new Job("test_job_1", 0, 10);
            break;
        case 1:
            j = new Job("test_job_2", 0, 10);
            break;
        case 2:
            j = new Job("test_job_3", 0, 10);
            break;
        default:
            j = new Job("test_job_4", 0, 10);
            break;
        }
        jobList.add(j);
        String notification = "NEW_JOB " + j.toString();
        notifyListeners(notification);
    }

    private class ConnectionHandler implements Runnable {

        String fromClient;
        Socket clientSocket;
        ServerDisplay display;

        public ConnectionHandler(Socket s) {
            this(s, null);
        }

        private ConnectionHandler(Socket s, ServerDisplay d) {
            clientSocket = s;
            nbClientsConnected++;
            display = d;
        }

        @Override
        public void run() {

//            System.out.println("Server receives connection from client.");
            try {
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                String outputLine;
//
//                String clientAddress = NODE_NUMBER + "";
//                NODE_NUMBER++;
//                boolean loop = true;
//                int imageSize;
//
                int bufferSize = 4096;
//
                String imageInfo = getNextImageInfo();
//                display.update(imageInfo, clientAddress + "", false);
                outputLine = "server_asks_for " + imageInfo;
                // ---------------- send MESSAGE 1
//                System.out.println("Server sends img index to client");
                out.println(outputLine);
//                System.out.println("Server done sending img index to client");
//
//                // Receive reply from client
//                // ---------------- receive MESSAGE 2
//                fromClient = in.readLine();
//                System.out.println("Server received from client: " + fromClient);
//                clientAddress = fromClient.split(" ")[1];
//
//                if (fromClient.startsWith("server")) {
//                    // The server already wrote the image, nothing more to do.
//
//                } else {
//                    // We must receive the image from the client.
//
//                    System.out.println("" + fromClient);
//                    // fromClient has the format "client 0x123456789, 127.0.0.42 rendered 1234 size 40000"
//                    imageSize = Integer.parseInt(fromClient.split(" ")[6]);
//
//                    // Actually receive the image and create the file
//                    File targetFile = new File(targetDirectory + "final_render" + imageInfo + ".png");
//
//                    DataInputStream dataStream = new DataInputStream(new BufferedInputStream(clientSocket.getInputStream()));
//                    FileOutputStream fileStream = new FileOutputStream(targetFile);
//                    byte[] bytes = new byte[bufferSize];
//
//                    // ----------------- send MESSAGE 3
//                    out.println("Server is ready for image");
//
//                    try {
//                        int count = 0;
//                        // -------------- receive DATA
//                        while ((count = dataStream.read(bytes)) != -1) {
//                            fileStream.write(bytes, 0, count);
//                        }
//                    } catch (java.net.SocketException e) {
//                        // TODO: what do we do in case of a socket exception ?
//                    } catch (EOFException e) {
//                        System.out.println("" + e);
//                    } catch (IOException e) {
//                        System.out.println("" + e);
//                    }
//
//                    fileStream.close();
//                    dataStream.close();
//                }
//                display.update(imageInfo, clientAddress, true);
//
//                avgCalc.add((int) System.currentTimeMillis());
//
//                String eta = getETA();
//                System.out.println("Node " + clientAddress + " f<" + imageInfo + "> " + eta);
//                notifyListeners("eta " + eta);
//
            } catch (IOException e) {
                System.out.println("Error in handling connection");
                nbClientsConnected--;
            }
        }

        /**
         * Estimate the remaining time, using the number of images needed and
         * the number of images already processed.
         *
         * @return a String representing the ETA.
         */
        private String getETA() {

            String result = "";
            return result;
        }
    }

    /**
     * Convert an amount of seconds to hours, minutes and seconds, and give the
     * result as a string formatted as "h:m:s"
     *
     * @param nbSec
     * @return a string representing that duration as hours, minutes and seconds
     */
    private static String convertSecToHMS(int nbSec) {
        String result = "";
        int nbHours = nbSec / 3600;
        if (nbHours > 0) {
            result += nbHours + ":";
        }

        int nbMin = (nbSec - 3600 * nbHours) / 60;
        if (nbSec > 60) {
            if (nbMin < 10) {
                result += "0";
            }
            result += nbMin + ":";
        }
        nbSec = nbSec - 3600 * nbHours - 60 * nbMin;
        if (nbSec < 10) {
            result += "0";
        }
        result += nbSec + "\"";
        return result;
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
        String command = words[0];
        String jobName;
        String jobID;
        int jobRank;
        int frame;

        switch (command) {
        case "DETAILS":
            jobID = words[1];
            sendJobDetails(jobID);
            break;
        case "Start":
            jobName = words[1];
            startJob(jobName, true);
            break;
        case "Stop":
            jobName = words[1];
            startJob(jobName, false);
            break;
        case "FILENAME_CHANGED":
            jobRank = Integer.valueOf(words[1]);
            String newJobName = words[2];
            Job oldJob = jobList.get(jobRank);
            Job newJob = new Job(newJobName, oldJob.getStartFrame(), oldJob.getEndFrame());
            jobList.set(jobRank, newJob);
            break;
        case "SET_FIRST_FRAME":
            jobRank = Integer.valueOf(words[1]);
            frame = Integer.valueOf(words[2]);
            jobList.get(jobRank).setFirstFrame(frame);
            break;
        case "SET_LAST_FRAME":
            jobRank = Integer.valueOf(words[1]);
            frame = Integer.valueOf(words[2]);
            jobList.get(jobRank).setLastFrame(frame);
            break;
        default:
            break;
        }
    }

    /**
     * Flag a job as started so that clients can take tasks.
     *
     * @param jobID the rank of the job we are starting.
     * @param mustStart true when the job must start, false when the job must
     * stop.
     */
    private void startJob(String jobName, boolean mustStart) {
        int jobRank = 0;
        for (Job j : jobList) {
            if (j.getName().equals(jobName)) {
                j.start();
                String notif;
                if (mustStart) {
                    notif = "JOB_STARTED " + jobRank;
                } else {
                    notif = "JOB_STOPPED " + jobRank;
                }
                notifyListeners(notif);
            }
            jobRank++;
        }
    }

    private void notifyListeners(String string) {
        for (Subscriber s : listeners) {
            s.update(string);
        }
    }

    /**
     * Send detailed information about the job identified by its name.
     *
     * @param jobName the name of the job, or "-1" in case of an error from the
     * GUI
     */
    private void sendJobDetails(String jobName) {
        if (jobName.equals("-1")) {
            return;
        }
        Job selectedJob = null;
        for (Job j : jobList) {
            if (j.getName().equals(jobName)) {
                selectedJob = j;
            }
        }
        if (selectedJob != null) {
            // Send info now
            String message = "JOB_DETAILS";
            message += " " + selectedJob.getName();
            message += " " + selectedJob.getFramesDetail();
            notifyListeners(message);
        }
    }
}
