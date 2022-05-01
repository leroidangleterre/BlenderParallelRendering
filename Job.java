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

    public Job(String filenameParam, int start, int end) {
        filename = filenameParam;
        status = JobStatus.NOT_STARTED;
        startFrame = start;
        endFrame = end;
    }

    public String getNextImageInfo() {
        return "lorem-ipsum 42";
    }

    public boolean isDone() {
        return this.status == JobStatus.FINISHED;
    }

}
