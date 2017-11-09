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
public class PredecessorCheckor extends Thread {

    private final Node thisNode;

    private boolean predecessorHBOK;

    public PredecessorCheckor(Node thisNode) {
        this.thisNode = thisNode;
        this.setName("predecessorChecker-Thread");
    }

    public void setPredecessorHBOK(boolean predecessorHBOK) {
        this.predecessorHBOK = predecessorHBOK;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (thisNode.getPredeccessor() != null) {
                    String hbMsg = "HB " + thisNode.getIp() + " " + thisNode.getPort();
                    thisNode.redirectMessage(hbMsg, thisNode.getPredeccessor());
                    System.out.println("HB Sent from " + thisNode.getID() + " to predecessor" + thisNode.getPredeccessor().getID());
                    Thread.sleep(30 * 1000);
                    if (!predecessorHBOK) {
                        System.out.println("No reply for HB. Setting predecessor to null");
                        thisNode.setPredecessor(null);
                    } else {
                        System.out.println("Predecessor " + thisNode.getPredeccessor().getID() + "is alive.");
                    }
                    predecessorHBOK = false;
                } else {  // no predecessor, sleep for 3 min
                    System.out.println("Predecessor null. Check again in 1 min");
                    Thread.sleep(60 * 1000);
                }
            } catch (InterruptedException ex) {
                System.out.println("predecessorChecker-Thread interrupted.");
            }
        }
    }

}
