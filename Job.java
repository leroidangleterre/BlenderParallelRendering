package blenderparallelrendering;

/**
 * This class describes a render job.
 * It contains a filename and a frame range.
 *
 * @author arthu
 */
public class Job {

    private String filename;
    private JobStatus status;
    private int startFrame;
    private int endFrame;
    private boolean active;

    public Job(String filenameParam, int start, int end) {
        filename = filenameParam;
        status = JobStatus.NOT_STARTED;
        startFrame = start;
        endFrame = end;
        active = false;
    }

    public String getNextImageInfo() {
        return "lorem-ipsum 42";
    }

    public boolean isDone() {
        return this.status == JobStatus.FINISHED;
    }

    public void start() {
        active = true;
    }

    /**
     * Get a simple text description of the job, without the current status.
     *
     * @return a simple text description of the job
     */
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

}
