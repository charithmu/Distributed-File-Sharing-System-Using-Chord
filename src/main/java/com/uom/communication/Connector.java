/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uom.communication;

/**
 *
 * @author erang
 */
public interface Connector {

    public void send(String OutgoingMessage, String ip, int port);

    public void sendToBS(String message);

    public void listen(int port);
}
