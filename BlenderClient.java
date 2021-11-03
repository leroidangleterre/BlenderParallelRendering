package blenderclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author arthu
 */
public class BlenderClient {

    public static void main(String[] args) {

        String serverAddress = "192.168.1.39";
        int serverPort = 65432;
        // TODO: puth this constant in a file so that it can be read by the server and the client
        final String TARGET_DIRECTORY = "D:\\Blender\\Dune\\png_rendered";

        System.out.println("Starting client.");
        String host;

        try {
            Socket socket = new Socket(serverAddress, serverPort);

            // Identify the host
            System.out.println("This client's address: " + socket.getInetAddress()
                    + ", server address: " + serverAddress);
            if (socket.getInetAddress().toString().equals(serverAddress)) {
                // This client runs on the same machine as the server.
                host = "server";
            } else {
                // This client does not run on the same machine as the server.
                host = "client";
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            ObjectOutputStream objectStream = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                String fromServer = reader.readLine();
                System.out.println("Client received from server: " + fromServer);
                int imageIndex = Integer.valueOf(fromServer.split(" ")[1]);

                // Test for blender file access
                File blenderExecutable = new File("C:\\Program Files\\Blender Foundation\\Blender 2.93\\blender.exe");
                System.out.println("testFile: " + blenderExecutable);
                System.out.println("Absolut path: " + blenderExecutable.getAbsolutePath());

                String imagePath = TARGET_DIRECTORY + "\\img_rendered_" + imageIndex + ".png";

                System.out.println("Writing image at " + imagePath);

                Runtime runtime = Runtime.getRuntime();
                String command = "/c/blender -b /d/Blender/Dune/Dune.blend -f " + imageIndex;
                System.out.println("Command: " + command);
                Process exec = runtime.exec(command);
                System.out.println("Waiting for Blender to finish");
                // wait for blender to finish
                exec.waitFor();
                System.out.println("Blender done rendering.");

                // Render the image
//                String command = "cmd /c " + blenderExecutable.getAbsolutePath() //"C:\\Program Files\\Blender Foundation\\Blender 2.93\\blender.exe\\"
//                        + " -b D:\\Blender\\Dune\\Dune.blend"
//                        //                        + " -o " + TARGET_DIRECTORY + "\\img_render#####"
//                        + " -f " + imageIndex;
//                String command = "C:\\Program Files\\Git\\git-bash.exe script.sh";
//                Process process = Runtime.getRuntime().exec(command);
//                System.out.println("After command launched");
//                int exitValue = process.waitFor();
//                System.out.println("exitValue = " + exitValue);
//                BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
//                String line = "";
//                while ((line = processReader.readLine()) != null) {
//                    System.out.println("Client Error: " + line);
//                }
//
//                System.out.println("Creating builder.");
//                ProcessBuilder builder = new ProcessBuilder(command);
//                System.out.println("Redirecting errors.");
//                builder.redirectErrorStream(true);
//                System.out.println("Starting process.");
//                final Process process = builder.start();
//                System.out.println("Done.");
//                System.out.println("Command: " + command);
//                Runtime rt = Runtime.getRuntime();
//                System.out.println("Executing command...");
//                Process pr = rt.exec(command);
//                System.out.println("Command done.");
//                pr.waitFor(); // Wait until process has terminated.
                // TODO
                File imageFile = new File(imagePath);
                long thousandMillisecs = 1000;
                Thread.sleep(thousandMillisecs);

                // Send a reply to the server
                String reply = host + " 0x123456789, 127.0.0.42 rendered " + imageIndex + " size 40000";
                System.out.println("Client sends " + reply + " to server.");
                out.println(reply);

                // A client on a distant machine must send the image to the server.
                if (!host.equals("server")) {
                    // Wait until the server sends "Server is ready for image"
                    fromServer = reader.readLine();
                    if (fromServer.equals("Server is ready for image")) {
                        // Send the image now.
                        FileInputStream fileStream = new FileInputStream(imageFile);
                        int b;
                        while ((b = fileStream.read()) != -1) {
                            objectStream.writeByte(b);
                        }
                        objectStream.flush();
                        fileStream.close();
                    } else {
                        System.out.println("Client error: server is not ready");
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(BlenderClient.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(BlenderClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
