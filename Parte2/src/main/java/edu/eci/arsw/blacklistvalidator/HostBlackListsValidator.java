/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.eci.arsw.blacklistvalidator;

import edu.eci.arsw.spamkeywordsdatasource.HostBlacklistsDataSourceFacade;
import edu.eci.arsw.threads.HostBlacklistThread;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author hcadavid
 */
public class HostBlackListsValidator {

    public static final int BLACK_LIST_ALARM_COUNT=5;

    public static AtomicInteger globalOccurrences = new AtomicInteger(0);
    /**
     * Check the given host's IP address in all the available black lists,
     * and report it as NOT Trustworthy when such IP was reported in at least
     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case.
     * The search is not exhaustive: When the number of occurrences is equal to
     * BLACK_LIST_ALARM_COUNT, the search is finished, the host reported as
     * NOT Trustworthy, and the list of the five blacklists returned.
     * @param ipaddress suspicious host's IP address.
     * @param N the number of threads that will search in a selected blacklist interval.
     * @return  Blacklists numbers where the given host's IP address was found.
     */
    public List<Integer> checkHost(String ipaddress, int N){
        HostBlacklistsDataSourceFacade skds = HostBlacklistsDataSourceFacade.getInstance();

        LinkedList<Integer> blackListOcurrences=new LinkedList<>();
        LinkedList<HostBlacklistThread> threads = new LinkedList<>();
        int serversPerThread = skds.getRegisteredServersCount() / N;

        int ocurrencesCount=0;
        
        int checkedListsCount=0;
        
        for (int i = 0; i < N; i++){
            int start = i * serversPerThread;
            int end = (i == N-1) ? skds.getRegisteredServersCount() : start + serversPerThread;

            HostBlacklistThread iThread = new HostBlacklistThread(start,end,ipaddress);

            threads.add(iThread);
            iThread.start();
        }

        for (HostBlacklistThread thread : threads) {
            try {
                thread.join();
                checkedListsCount += thread.getReviewedListCounter();
                blackListOcurrences.addAll(thread.getBlacklistAppearances());
            } catch (InterruptedException e) {
                Logger.getLogger(HostBlackListsValidator.class.getName()).log(Level.SEVERE,null,e);
            }
        }

        ocurrencesCount = blackListOcurrences.size();

        if (ocurrencesCount >= BLACK_LIST_ALARM_COUNT){
            skds.reportAsNotTrustworthy(ipaddress);
        }
        else{
            skds.reportAsTrustworthy(ipaddress);
        }                
        
        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{checkedListsCount, skds.getRegisteredServersCount()});
        
        return blackListOcurrences;
    }

    public AtomicInteger getGlobalOccurrences() {
        return globalOccurrences;
    }

    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());

}
