package blenderparallelrendering;

import java.util.ArrayList;

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
    private boolean active;

    // These three lists must be changed in parallel
    private ArrayList<Integer> frameNumberList;
    private ArrayList<Status> frameStatusList;
    private ArrayList<String> hostList;

    public Job(String filenameParam, int start, int end) {
        filename = filenameParam;
        status = Status.NOT_STARTED;
        startFrame = start;
        endFrame = end;
        active = false;
        initImageList();
    }

    public String getNextImageInfo() {
        return "lorem-ipsum 42";
    }

    public boolean isDone() {
        return this.status == Status.FINISHED;
    }

    public void start() {
        active = true;
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
            framesInfo += frameNumberList.get(rank) + ":";
            framesInfo += frameStatusList.get(rank) + ":";
            framesInfo += hostList.get(rank) + " ";
        }
        return framesInfo;
    }
}
