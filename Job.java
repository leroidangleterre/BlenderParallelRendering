package blenderparallelrendering;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class describes a render job.
 * It contains a filename and a frame range.
 *
 * @author arthu
 */
public class Job {

    private String filename;
    private Status status;
    private int startFrame;
    private int endFrame;

    // These three lists must be changed in parallel
    private ArrayList<Integer> frameNumberList;
    private ArrayList<Status> frameStatusList;
    private ArrayList<String> hostList;

    public Job(String filenameParam, int start, int end) {
        filename = filenameParam;
        status = Status.NOT_STARTED;
        startFrame = start;
        endFrame = end;
        initImageList();
        Path file = Paths.get(filename);
        try {
            BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        } catch (NoSuchFileException ex) {
            System.out.println("No file found");
        } catch (IOException ex) {
            Logger.getLogger(Job.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getNextImageInfo() {

        if (this.isActive()) {

            int rank = 0;
            for (Status s : frameStatusList) {
                if (s.equals(Status.NOT_STARTED)) {
                    // We found an image that has yet to be assigned.
                    String info;
                    info = filename + " " + /*date + " " +*/ frameNumberList.get(rank);
                    frameStatusList.set(rank, Status.IN_PROGRESS);
                    rank++;
                    System.out.println("Job.getNextImageInfo() returning " + info);
                    return info;
                }
            }
        }
        return "none";
    }

    public boolean isDone() {
        return this.status == Status.FINISHED;
    }

    public void start() {
        this.status = Status.IN_PROGRESS;
    }

    public void stop() {
        this.status = Status.NOT_STARTED;
    }

    public boolean isActive() {
        return this.status == Status.IN_PROGRESS;
    }

    /**
     * Get a simple text description of the job, without the current status.
     *
     * @return a simple text description of the job
     */
    @Override
    public String toString() {
        return filename + " " + startFrame + " " + endFrame;
    }

    /**
     * Get the name of the file associated to this job.
     *
     * @return the name of the file associated to this job
     */
    public String getName() {
        return filename;
    }

    /**
     * Get the index of this job's first frame.
     *
     * @return the first frame of this job
     */
    public int getStartFrame() {
        return this.startFrame;
    }

    /**
     * Get the index of this job's last frame.
     *
     * @return the last frame of this job
     */
    public int getEndFrame() {
        return this.endFrame;
    }

    public void setFirstFrame(int frame) {
        this.startFrame = frame;
        initImageList();
    }

    public void setLastFrame(int frame) {
        this.endFrame = frame;
        initImageList();
    }

    /**
     * Build the list of all images
     *
     */
    private void initImageList() {
        frameNumberList = new ArrayList<>();
        frameStatusList = new ArrayList<>();
        hostList = new ArrayList<>();

        for (int imageIndex = startFrame; imageIndex <= endFrame; imageIndex++) {
            frameNumberList.add(imageIndex);
            frameStatusList.add(Status.NOT_STARTED);
            hostList.add("none");
        }
    }

    /**
     * Return a string containing info about the status of each frame
     *
     * @return a string containing detailed status on each frame
     */
    public String getFramesDetail() {
        // Requested info for each frame:
        // frame number, status, host

        String framesInfo = "";
        for (int rank = 0; rank < frameNumberList.size(); rank++) {
            framesInfo += "FRAME:";
            framesInfo += frameNumberList.get(rank) + ":";
            framesInfo += frameStatusList.get(rank) + ":";
            framesInfo += hostList.get(rank) + " ";
        }
        return framesInfo;
    }
}
