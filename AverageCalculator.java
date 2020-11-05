package blenderparallelrendering;

import java.util.ArrayList;

/**
 * Compute the average time needed per image. We only use events no older than a
 * given maximum.
 *
 * @author arthu
 */
public class AverageCalculator {

    // The events more recent than that duration are taken into account.
    private int nbEventsMax = 20;

    // Dates of completion of the most recent images.
    private ArrayList<Integer> dateList;

    public AverageCalculator() {
        dateList = new ArrayList<>();
        dateList.add((int) (System.currentTimeMillis()));
        System.out.println("AverageCalculator init at date " + dateList.get(0));
    }

    /**
     * Take a new datapoint. Remove an old one if the max amount of datapoints
     * is reached.
     *
     * @param newDate the new datapoint, in milliseconds from 01-01-1970.
     */
    public void add(int newDate) {
        dateList.add(newDate);
        trim();
    }

    /**
     * Compute and return the average value in millisecond. In later versions,
     * the values shall be weighted so that the most recent ones have more
     * impact on the result.
     */
    public int getAverage() {
        int currentDateInMs = (int) System.currentTimeMillis();

        int duration = currentDateInMs - dateList.get(0);
        int nbPoints = dateList.size();
        int average = duration / (dateList.size() - 1);

        return average;
    }

    /**
     * Increase the amount of datapoints allowed.
     *
     */
    public void increaseSetSize() {
        nbEventsMax++;
    }

    /**
     * Decrease the amount of datapoints allowed. Trim the list if needed. At
     * least one datapoint will be maintained.
     */
    public void decreaseSetSize() {
        if (nbEventsMax > 1) {
            nbEventsMax--;
        }
        trim();
        System.out.println("AverageCalculator has " + nbEventsMax + " elements max, currently " + dateList.size());
    }

    /**
     * Make sure the list does not have too many elements.
     *
     */
    private void trim() {
        while (dateList.size() >= nbEventsMax) {
            dateList.remove(0);
        }
    }
}
