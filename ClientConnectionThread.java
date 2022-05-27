package blenderparallelrendering;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author arthu
 */
public class ClientConnectionThread extends Thread {

    private String serverIP;
    private InetAddress serverAddress;
    private Socket socket;
    private int port;
    private boolean isActive;

    private long sleepDurationInMillis = 1000;
    private long imageRenderSimulationTime = 4000;

    private final String NEED_FILE = "need_file";
    private File localFile; // The local version of the .blend file, as received from the server

    public ClientConnectionThread(String serverIPParam, int portParam) {
        serverIP = serverIPParam;
        port = portParam;
        isActive = false;
        localFile = null;
    }

    @Override
    public void run() {

        isActive = true;

        while (isActive) {
            socket = new Socket();
            try {

                serverAddress = InetAddress.getByName(serverIP);
                socket.connect(new InetSocketAddress(serverAddress, port));
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String fromServer = in.readLine();
                String jobName = fromServer.split(" ")[1];
                if (jobName.equals("none")) {
                    System.out.println("Client: Nothing available.");
                    Thread.sleep(sleepDurationInMillis);
                } else if (fromServer.contains("error")) {
                    System.out.println("ClientConnectionThread error when receiving from server: <" + fromServer + ">");
                    Thread.sleep(sleepDurationInMillis);
                } else {
                    int frame = Integer.valueOf(fromServer.split(" ")[2]);
                    System.out.println("Client received from server: " + jobName + ", frame " + frame);

                    workOnFrame(jobName, frame, in);

                }

            } catch (UnknownHostException ex) {
                System.out.println("Unknown Host Exception.");
                isActive = false;
            } catch (IOException ex) {
                System.out.println("IO Exception: server is unavailable.");
            } catch (InterruptedException ex) {
                System.out.println("ClientConnectionThread interrupted");
            }
        }
    }

    /**
     * Set the thread to stop before next loop.
     *
     */
    protected void flagStop() {
        System.out.println("Client being flagged to stop.");
        isActive = false;
    }

    /**
     * Render an image from the animation.
     * The client must check the file's existence and download any required
     * material.
     *
     * @param jobName the path to the .blend file
     * @param in the communication channel with the server (used to request and
     * get a new file)
     */
    private void workOnFrame(String jobName, int frame, BufferedReader in) {
        System.out.println("Client working on job " + jobName + ", frame " + frame);
        try {
            // Check the .blend file (name and date), send a request if necessary

            String escapedJobName;
            String[] split;
            String filename;
            Path userDir;
            Path localBlendFiles;

            // Isolate the filename from the folder (e.g. isolate "myfile.blend" from input "D\Blender\project\myFile.blend")
            // Double the backslashes in the filename
            escapedJobName = jobName.replace("\\", "\\\\");
            // Split on double backslashes
            split = escapedJobName.split("\\\\");
            filename = split[split.length - 1];

            userDir = Paths.get(System.getProperty("user.dir"));
            localBlendFiles = userDir.resolve("localBlendFiles");

            File file;
            String separator;
            if (isWindows()) {
                separator = "\\\\";
            } else {
                separator = "/";
            }
            file = new File(localBlendFiles + separator + filename);

            if (!file.exists()) {
                System.out.println("Client does not have the file <" + file + ">");
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                System.out.println("Client sending file request to server");
                out.println(NEED_FILE + " " + jobName);

                // Receive file from server.
                try {
                    System.out.println("Client opening input stream.");
                    DataInputStream dataStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                    FileOutputStream fileStream = new FileOutputStream(file);
                    int bufferSize = 4096;
                    byte[] bytes = new byte[bufferSize];
                    int count = 0;
                    while ((count = dataStream.read(bytes)) != -1) {
                        fileStream.write(bytes, 0, count);
                    }
                    fileStream.close();
                    dataStream.close();
                    System.out.println("Client has received file.");
                } catch (IOException e) {
                    System.out.println("IO exception on client while reading file from server");
                }
            }

            // Render the image
            System.out.println("Client working on file " + file);
            renderImage(file, frame);
            // Send the image to the server
            System.out.println("Client done.");
        } catch (NoSuchFileException ex) {
            System.out.println(ex);
        } catch (IOException ex) {
            Logger.getLogger(ClientConnectionThread.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Client error.");
        }
    }

    /**
     * Tell if the client is running on Windows or another OS; assume Linux if
     * not Windows.
     *
     * @return true when the client is running on Windows, false for any other
     * OS.
     */
    private boolean isWindows() {

        Properties props = System.getProperties();
        String osProp = (String) (props.get("os.name"));
        if (osProp.startsWith("Win")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Compute a single image using Blender.
     *
     * @param file the .blend file containing the animation
     * @param frame the index of the single frame that we want to render
     */
    private void renderImage(File file, int frame) {

        try {
            String pathToScript = "C:\\Users\\arthu\\Documents\\Programmation\\Java\\BlenderParallelRendering\\script.sh";
            String pathToSh = "C:\\Program Files\\Git\\bin\\sh";
            String[] cmd = new String[]{pathToSh, pathToScript};
            Process p = Runtime.getRuntime().exec(cmd);

            int returnValue = p.waitFor();
            System.out.println("return value: " + returnValue);

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Script output: " + line);
            }
            Thread.sleep(imageRenderSimulationTime);
        } catch (IOException ex) {
            System.out.println("IOException when running script by client");
            Logger.getLogger(ClientConnectionThread.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            System.out.println("Interrupted Exception when running script by client");
            Logger.getLogger(ClientConnectionThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
