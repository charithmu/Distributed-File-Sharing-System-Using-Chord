/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uom.chord;

/**
 *
 * @author erang
 */
public interface FingerTable {
    public Node getNode(int key);
    public Node getNodeAt(int index);
    public Node[] getFingerEntries();
    public int searchEntries(int key);
    public void updateEntry(int index, Node node);
    //public void removeEntry(int key);
    public Node getClosestPredecessorToKey(int key);
    public Node getEntryByIndex(int index);
    public Node getClosestPredecessorToKey(int myID, int destKey);
}
