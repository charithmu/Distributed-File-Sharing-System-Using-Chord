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
public class FingerTable{

    Node[] fingerEntries;
    private int maxEntries;

    public FingerTable(int maxEntries) {
        this.maxEntries = maxEntries;
        this.fingerEntries = new Node[maxEntries];
    }

    public Node[] getFingerEntries() {
        return this.fingerEntries;
    }

    public int searchEntries(int key) {
        for (int i = 0; i < maxEntries; i++) {
            if (fingerEntries[i].getID() == key) {
                return i;
            }
        }
        return -1;
    }

    public Node getNode(int key) {
        for (Node node : fingerEntries) {
            if (node == null) {
                System.out.println("node is null");
            } else if (node.getID() == key) {
                return node;
            }
        }
        return null;
    }

    public void updateEntry(int index, Node node) {
        this.fingerEntries[index] = node;
    }

    public Node getClosestPredecessorToKey(int Destkey) {
        int maxBeforeKey = Integer.MIN_VALUE;
        int maxKeyIndex = -1;
        boolean found = false;
        Node ClosestPredecessor = null;

        for (int i = 0; i < maxEntries; i++) {
            int key = fingerEntries[i].getID();
            if (key < Destkey && key > maxBeforeKey) {
                maxBeforeKey = key;
                maxKeyIndex = i;
                found = true;
            }
        }

        if (found) {
            ClosestPredecessor = this.fingerEntries[maxKeyIndex];
        }
        if (ClosestPredecessor == null) {
            System.out.println("NO PRED");
        }
        return ClosestPredecessor;
    }

    public Node getClosestPredecessorToKey(int myID, int destKey) {
        for (int i = maxEntries - 1; i >= 0; i--) {
            int fingerKey = fingerEntries[i].getID();
            if (myID < destKey) {
                if (fingerKey <= destKey) {
                    return fingerEntries[i];
                }
            } else if ((myID < fingerKey && fingerKey < Node.MAX_NODES) || (0 <= fingerKey && fingerKey <= destKey)) {
                return fingerEntries[i];
            }
        }
        return null;
    }

    /*@Override
    public void removeEntry(int key) {
        this.fingerEntries.remove(key);
    }*/
    public Node getNodeAt(int index) {
        if (index >= 0 && index < maxEntries) {
            return fingerEntries[index];
        }
        return null;
    }

    public Node getEntryByIndex(int index) {
        return fingerEntries[index];
    }

}
