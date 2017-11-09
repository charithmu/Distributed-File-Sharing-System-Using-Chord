/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uom.chord;

import com.uom.view.GUI;


/**
 *
 * @author erang
 */
public interface Node {

    public void joinNetwork();

    public boolean leaveNetwork();

    public void initialize();

//    public void routeMessge(String message, int key);

    public Node getSuccessor();

    public Node getPredeccessor();

    public void setSuccessor(Node successor);

    public void setPredecessor(Node predecessor);

    public String getIp();

    public String getUserName();

    public int getPort();

    public String getBSip();

    public int getBSport();

    public void handleMessage(String message, String incomingIP);

    public FingerTable getFingerTable();

    public int getID();

    public Node findSuccessorOf(int finger, int id, String originIP, int originPort, boolean reqFromFingerFixer);

    public void redirectMessage(String message, Node next);

    public void echo(String output);
    
    public GUI getGUI();
    
    public void setGUI(GUI gui);
    
    public void distributeFileMetadata();

}
