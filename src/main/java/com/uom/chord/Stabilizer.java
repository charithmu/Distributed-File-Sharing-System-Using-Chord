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
public class Stabilizer extends Thread {

    private final Node thisNode;
    private Node newPredessor = null;
    private boolean predMsgSent;
    private boolean nullPredecessor;

    public Stabilizer(Node thisNode) {
        this.thisNode = thisNode;
        this.setName("Stabilizer-Thread");
    }

    public void setNewPredessor(Node newPredessor) {
        this.newPredessor = newPredessor;
    }

    public void setNullPredecessor(boolean nullPredecessor) {
        this.nullPredecessor = nullPredecessor;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (thisNode.getSuccessor() == null) {
                    System.out.println("Successor is null. Stabilizer will run in 10 seconds");
                    Thread.sleep(10 * 1000);
                }
                if (thisNode.getSuccessor() != null) {
//                    System.out.println("Stabilizing...\n");
                    if (!predMsgSent) {
                        String msg = "GET_PRED " + thisNode.getIp() + " " + thisNode.getPort();
                        thisNode.redirectMessage(msg, thisNode.getSuccessor());
                        predMsgSent = true;

                    }
                    Thread.sleep(20 * 1000);
                }
            } catch (InterruptedException ex) {
                if (predMsgSent) {
                    boolean notify = false;
                    if (newPredessor != null) {
//                        System.out.println("GET_PRED Success");
                        int predOfSuccessorID = newPredessor.getID();
                        int currentSuccessorID = thisNode.getSuccessor().getID();
                        // this node and successor are in opposite side of 0
                        if (currentSuccessorID < thisNode.getID()) {
                            if ((thisNode.getID() < predOfSuccessorID && predOfSuccessorID < Node.MAX_NODES) || (0 <= predOfSuccessorID && predOfSuccessorID < currentSuccessorID)) {
                                thisNode.setSuccessor(newPredessor);
                                notify = true;
                                System.out.println("Update Successor of" + thisNode.getID() + " to " + predOfSuccessorID);
                            }
                        } else if (thisNode.getID() < predOfSuccessorID && predOfSuccessorID < currentSuccessorID) { // normal successor
                            thisNode.setSuccessor(newPredessor);
                            notify = true;
                            System.out.println("Update Successor of" + thisNode.getID() + " to " + predOfSuccessorID);
                        }
                    } else if (nullPredecessor) {
                        System.out.println("Sucessors predecessor is null, Notifying successor to update predecessor as me");
                        notify = true;
                    }
                    if (notify) {
                        String msg = "NOTIFY_S " + thisNode.getIp() + " " + thisNode.getPort();
                        thisNode.redirectMessage(msg, thisNode.getSuccessor());
                    }
                    newPredessor = null;
                    predMsgSent = false;
                    nullPredecessor = false;
                }
            }
        }
    }

}
