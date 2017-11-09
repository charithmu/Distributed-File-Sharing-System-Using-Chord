/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uom.chord;

/**
 *
 * @author Irfad Hussain
 */
public class FingerFixer extends Thread {

    private static int fingerToFixNext = -1;

    private Node thisNode;
    private FingerTable fingerTable;
    private boolean[] waitingForReply;
    private long[] lastFindSIssue;
    boolean fileDistributed;

    public FingerFixer(Node thisNode) {
        this.setName("FingerFixer-Thread");
        this.thisNode = thisNode;
        this.fingerTable = thisNode.getFingerTable();
        this.waitingForReply = new boolean[NodeImpl.MAX_FINGERS];
        this.lastFindSIssue = new long[NodeImpl.MAX_FINGERS];
        this.fileDistributed = false;
    }

    public void setWaitingForReply(int index, boolean waitStatus) {
        this.waitingForReply[index] = waitStatus;
    }

    public void setLasFindSIssue(int index, long issueTime) {
        this.lastFindSIssue[index] = issueTime;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (thisNode.getSuccessor() == null) {
                    System.out.println("Successor is null. FingerFixer will run in 10 seconds");
                    Thread.sleep(10 * 1000);
                }
                if (thisNode.getSuccessor() != null) {  // successor can still be null after wake
                    fingerToFixNext++;
                    if (fingerToFixNext >= NodeImpl.MAX_FINGERS) {
//                        System.out.println("FingerFixer New iteration...");
                        if (!fileDistributed) {
                            thisNode.distributeFileMetadata();
                            fileDistributed = true;
                        }
                        Thread.sleep(5000);
                        fingerToFixNext = 0;
                    }
                    if (!waitingForReply[fingerToFixNext] || (waitingForReply[fingerToFixNext] && lastFindSIssue[fingerToFixNext] + 2 * 60 * 1000 < System.currentTimeMillis())) {
                        this.waitingForReply[fingerToFixNext] = false;
                        this.lastFindSIssue[fingerToFixNext] = System.currentTimeMillis();
                        Node fingerEntry = thisNode.findSuccessorOf(fingerToFixNext, (thisNode.getID() + (int) Math.pow(2, fingerToFixNext)) % NodeImpl.MAX_NODES, thisNode.getIp(), thisNode.getPort(), true);
                        if (fingerEntry != null) {
//                            System.out.println("FixFinger: Update finger " + fingerToFixNext + " of " + thisNode.getID() + " from  " + fingerTable.getNodeAt(fingerToFixNext).getID() + " to " + fingerEntry.getID());
                            fingerTable.updateEntry(fingerToFixNext, fingerEntry);
                            thisNode.getGUI().UpdateFingerTable(fingerToFixNext, fingerEntry);
                        }
                    }
                    Thread.sleep(10 * 1000);
                }
            } catch (InterruptedException ex) {
            }
        }
    }

}
