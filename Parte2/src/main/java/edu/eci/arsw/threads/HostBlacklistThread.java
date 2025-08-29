package edu.eci.arsw.threads;

import edu.eci.arsw.blacklistvalidator.HostBlackListsValidator;
import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;

import java.util.LinkedList;


/**
 * Class that represents a thread responsible for checking a specific range of blacklist servers
 * for the presence of a given IP host address.
 *
 * This class extends {@link Thread} and is designed to be used in a multi-threaded
 * environment to efficiently validate an IP address against a large number of blacklists.
 */
public class HostBlacklistThread extends Thread {
    private int start, end;
    private String ipHostAddress;
    private int appearanceCounter, reviewedListCounter;
    private HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();
    LinkedList<Integer> blacklistAppearances = new LinkedList<>();

    /**
     * Constructs a new HostBlacklistThread with a specific range and IP address.
     *
     * @param start         The starting index (inclusive) of the blacklist range.
     * @param end           The ending index (exclusive) of the blacklist range.
     * @param ipHostAddress The IP host address to check.
     */
    public HostBlacklistThread(int start, int end, String ipHostAddress){
        this.start = start;
        this.end = end;
        this.ipHostAddress = ipHostAddress;
        this.appearanceCounter = 0;
        this.reviewedListCounter = 0;
    }

    /**
     * The main execution method for the thread. It iterates through the assigned
     * range of blacklists and checks if the IP address is present. The loop
     * terminates early if the IP is found a certain number of times, as defined
     * by BLACK_LIST_ALARM_COUNT.
     */
    @Override
    public void run() {
        for (int i = start; i < end && appearanceCounter < HostBlackListsValidator.BLACK_LIST_ALARM_COUNT; i++) {
            reviewedListCounter++;
            if (skds.isInBlackListServer(i, ipHostAddress)) {
                blacklistAppearances.add(i);
                appearanceCounter++;
            }
        }
    }

    /**
     * Retrieves the number of blacklists reviewed by this thread.
     *
     * @return The total count of reviewed lists.
     */
    public int getReviewedListCounter() {
        return reviewedListCounter;
    }

    /**
     * Retrieves the list of indices where the IP address was found in a blacklist.
     *
     * @return A {@link LinkedList} of integers representing the indices of the
     * blacklists that contain the IP address.
     */
    public LinkedList<Integer> getBlacklistAppearances() {
        return blacklistAppearances;
    }
}
