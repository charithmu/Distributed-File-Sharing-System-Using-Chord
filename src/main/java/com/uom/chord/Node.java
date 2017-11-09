/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.uom.chord;

import Chord.SimpleNeighbor;
import com.uom.communication.RestConnector;
import com.uom.view.GUI;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author erang
 */
public final class Node {

    private GUI gui;
    public static final int MAX_FINGERS = 4;
    public static final int MAX_NODES = 1024;//(int) Math.pow(2, MAX_FINGERS);

    private final String BSip;
    private final int BSport;

    private final String ip;
    private final int port;
    private final String username;
    private final int id;

//    private SocketConnector connector;
    private RestConnector connector;
    private FingerTable fingerTable;
    private Node successor;
    private Node predecessor;

    private ArrayList<SimpleNeighbor> neighborList;

    private Map<String, Node> metaData;//these are the pointers
    private ArrayList<String> files;

    private Stabilizer stabilizer;
    private FingerFixer fingerFixer;
    private PredecessorCheckor predecessorCheckor;

    public Node(String username, String ip, int port, String BSip, int BSport, GUI gui, boolean MainOrDummy) {

        this.username = username;
        this.ip = ip;
        this.port = port;
        this.BSip = BSip;
        this.BSport = BSport;
        this.id = getHash(this.ip + this.port);

        if (MainOrDummy) {
            fingerTable = new FingerTable(MAX_FINGERS);
            metaData = new HashMap<>();
            files = new ArrayList<>();
            neighborList = new ArrayList<>();

//            this.connector = new SocketConnector(this);
            this.connector = new RestConnector(this);

            this.stabilizer = new Stabilizer(this);
            this.fingerFixer = new FingerFixer(this);
            this.predecessorCheckor = new PredecessorCheckor(this);
            this.gui = gui;
            initialize();
        }
    }

    public Node(String username, String ip, int port, boolean proxy) {
        fingerTable = new FingerTable(MAX_FINGERS);
        metaData = new HashMap<>();
        files = new ArrayList<>();
        neighborList = new ArrayList<>();

        this.username = username;
        this.ip = ip;
        this.port = port;
        this.BSip = null;
        this.BSport = 0;
        this.id = getHash(this.ip + this.port);
    }

    public void initialize() {
        populateWithFiles();
        this.connector.listen(port);

        new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(10 * 1000);
                    distributeFileMetadata();
                } catch (InterruptedException ex) {
                    System.err.println(ex);
                }

            }
        }.start();
    }

    public String getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }

    public String getBSip() {
        return BSip;
    }

    public int getBSport() {
        return BSport;
    }

    public String getUserName() {
        return this.username;
    }

    public int getID() {
        return this.id;
    }

    public int getthePort() {
        return port;
    }

    public Node getSuccessor() {
        return this.successor;
    }

    public Node getPredeccessor() {
        return this.predecessor;
    }

    public void setSuccessor(Node successor) {
        this.successor = successor;
        gui.UpdateSuccessor(successor);
    }

    public void setPredecessor(Node predecessor) {
        this.predecessor = predecessor;
        gui.UpdatePredecessor(predecessor);
    }

    public void redirectMessage(String message, Node next) {
        connector.send(message, next.getIp(), next.getPort());
//        echo("redirecting message to: " + next.getPort() + "(" + message + ")");
    }

    public FingerTable getFingerTable() {
        return fingerTable;
    }

    /**
     *
     * @return
     */
    public GUI getGUI() {
        return this.gui;
    }

    public void setGUI(GUI gui) {
        this.gui = gui;
    }

    public void distributeFileMetadata() {
        for (String file : files) {
            int hashKey = getHash(file);
            String message = "REGMD " + this.ip + " " + this.port + " " + this.username + " " + "@" + file;//register meta data
            if (fingerTable == null) {
                System.out.println("Finger Table is null");
            }

            Node receiver = fingerTable.getNode(hashKey);
            if (receiver == null) {
                receiver = fingerTable.getClosestPredecessorToKey(hashKey);
            }
            if (receiver == null) {
                receiver = this.successor;
            }
            if (receiver != null) {
                connector.send(message, receiver.getIp(), receiver.getPort());
            } else {
                gui.echo("No receiver found for metadata distribution");
            }
        }
    }

    private void insertFileMetadata(String file, Node Mnode) {
        this.metaData.put(file, Mnode);
        gui.updateMetadataTable(file, metaData);
    }

    public static String getMyIP() {
        try {
            final DatagramSocket socket = new DatagramSocket();
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException ex) {
            System.err.println(ex);
            return null;
        }
    }

    public void populateWithFiles() {
        ArrayList<String> filelist = new ArrayList<>(Arrays.asList("Adventures of Tintin", "Jack and Jill", "Glee", "The Vampire Diarie", "King Arthur", "Windows XP", "Harry Potter", "Kung Fu Panda", "Lady Gaga", "Twilight", "Windows 8", "Mission Impossible", "Turn Up The Music", "Super Mario", "American Pickers", "Microsoft Office 2010", "Happy Feet", "Modern Family", "American Idol", "Hacking for Dummies"));
        String text = "<html>";
        for (int i = 0; i < 3; i++) {
            int rand = new Random().nextInt(filelist.size());
            String file = filelist.get(rand);
            files.add(file.toLowerCase());
            text += "My file " + getHash(file) + " is: " + file + "<br>";
            filelist.remove(rand);
        }
        text += "</html>";
        if (gui != null) {
            gui.updateFileLabel(text);
        }
    }

    public void registerToNetwork() {

        String registerMessage = " " + "REG" + " " + ip + " " + Integer.toString(port) + " " + username;
        int length = registerMessage.length() + 4;

        registerMessage = String.join("", Collections.nCopies(4 - (Integer.toString(length).length()), "0"))
                + Integer.toString(length) + registerMessage;

//        gui.echo("register message - (" + registerMessage + ")");
        connector.sendToBS(registerMessage);

    }

    public void unregisterFromNetwork() {

        echo("Unregistering from network as: (" + username + ")");

        String unregisterMessage = " " + "UNREG" + " " + ip + " " + Integer.toString(port) + " " + username;
        int length = unregisterMessage.length() + 4;

        unregisterMessage = String.join("", Collections.nCopies(4 - (Integer.toString(length).length()), "0")) + Integer.toString(length) + unregisterMessage;

        connector.sendToBS(unregisterMessage);
    }

    public void joinNetwork() {

        sendMessageToSuccessor();
//        sendMessageToPredecessor();//not implemented yet
    }

    public boolean leaveNetwork() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void sendMessageToSuccessor() {
        String message = "FS " + this.id + " " + this.ip + " " + this.port;
        this.connector.send(message, this.neighborList.get(0).getIp(), this.neighborList.get(0).getPort());
//        echo("Sending message to neighbor (" + this.neighborList.get(0).getPort() + "), Routing to self (" + message + ")");
    }

//    public void sendMessageToPredecessor() {
//        //not implemented yet
//    }
    public void search(String searchString) {
        String searchQuery = "SER" + " " + ip + " " + port + " @" + searchString;
//        int length = searchQuery.length() + 4;

//        searchQuery = String.join("", Collections.nCopies(4 - (Integer.toString(length).length()), "0")) + Integer.toString(length) + searchQuery;
        //check whether I have the file.
        if (files.contains(searchString)) {
            foundFile(searchString, this);
        } else if (this.searchMetaData(searchString) != null) {
            Node receiver = this.searchMetaData(searchString);
            gui.echo("Sending message: " + searchQuery + " (to " + receiver.getIp() + ":" + receiver.getPort() + ") - found this node in meta data");
            connector.send(searchQuery, receiver.getIp(), receiver.getPort());
        } //else do this
        else {
            Node receiver = null;
            int hashKey = getHash(searchString);
            receiver = fingerTable.getNode(hashKey);
            if (receiver == null) {
                receiver = fingerTable.getClosestPredecessorToKey(hashKey);
            }
            if (receiver == null) {
                receiver = this.successor;
            }
            if (receiver != null) {
                gui.echo("Sending message: " + searchQuery + " (to " + receiver.getIp() + ":" + receiver.getPort() + ")");
                connector.send(searchQuery, receiver.getIp(), receiver.getPort());
            } else {
                gui.updateDisplay("No receiver found");
            }
        }
    }

    private void foundFile(String searchString, Node result) {
        gui.updateDisplay("Found the file \"" + searchString + "\" on node " + result.getIp() + " : " + result.getPort() + " (" + result.getUserName() + ")");
    }

    public Node searchMetaData(String file) {
        if (metaData.containsKey(file)) {
            return this.metaData.get(file);
        } else {
            return null;
        }
    }

    /*
    BS Messages:
    
    length REG IP_address port_no username
    length REGOK no_nodes IP_1 port_1 IP_2 port_2
    length UNREG IP_address port_no username
    length UNROK value
    
    Node Messages:
    
    length JOIN IP_address port_no
    length JOINOK value
    length LEAVE IP_address port_no
    length LEAVEOK value
    length SER IP port file_name hops
    length SEROK no_files IP port hops filename1 filename2 ... ...
    
    length ERROR
     */
    public void handleMessage(String message, String incomingIP) {
//        echo(message);//this is also implemented in listener

        String[] messageList = message.split(" ");

        if (null != messageList[0] && messageList.length > 1) {

//            echo("message received: (" + messageList[0] + ")");
            switch (messageList[0]) {
                case "FS"://find successor
                    echo("message received: (" + messageList[0] + ")");

                    int key = Integer.parseInt(messageList[1]);

                    if (this.getSuccessor() == null) {

                        //set new node as my successor
                        Node tempSuccessor = new Node(null, messageList[2], Integer.parseInt(messageList[3]), true);
                        this.setSuccessor(tempSuccessor);
//                        gui.echo(this.getPort() + ": my successor is :- " + this.successor.getIp() + ":" + this.successor.getPort());
//                        gui.echo("succ null");
                        ///send me as the successor for new node
                        String tempMsg = "US " + this.getIp() + " " + this.getPort();
                        connector.send(tempMsg, messageList[2], Integer.parseInt(messageList[3]));

                    } else {
                        boolean routMessage = true;
                        if (this.id < successor.getID()) {  // normal successor
                            if (id < key && key < successor.getID()) {
                                //Ask to update new nodes successor to my successor
                                //"US <successorIP> <successorPort>"
                                String tempMsg = "US " + successor.getIp() + " " + successor.getPort();
                                connector.send(tempMsg, messageList[2], Integer.parseInt(messageList[3]));

                                Node tempSuccessor = new Node(null, messageList[2], Integer.parseInt(messageList[3]), true);
                                this.setSuccessor(tempSuccessor);
//                                gui.echo(this.getPort() + ": my successor is : " + this.successor.getPort());

                                //request the finger tabel from my successor
                                //"RFT <myIP> <myPort>"
                                /*tempMsg = "RFT " + this.getIp() + " " + this.getPort();
                                socketConnector.send(tempMsg, incomingIP, Integer.parseInt(messageList[3]));
                                gui.echo(this.getPort() + ": request finger table : (" + tempMsg + ")");*/
                                routMessage = false;
                            }
                        } else if ((id < key && key < MAX_NODES) || (0 <= key && key < successor.getID())) {
                            String tempMsg = "US " + successor.getIp() + " " + successor.getPort();
                            connector.send(tempMsg, messageList[2], Integer.parseInt(messageList[3]));

                            Node tempSuccessor = new Node(null, messageList[2], Integer.parseInt(messageList[3]), true);
                            this.setSuccessor(tempSuccessor);
//                            gui.echo(this.getPort() + ": my successor is : " + this.successor.getPort());

                            //request the finger tabel from my successor
                            //"RFT <myIP> <myPort>"
                            /*tempMsg = "RFT " + this.getIp() + " " + this.getPort();
                                socketConnector.send(tempMsg, incomingIP, Integer.parseInt(messageList[3]));
                                gui.echo(this.getPort() + ": request finger table : (" + tempMsg + ")");*/
                            routMessage = false;
                        }
                        if (routMessage) {
                            Node next = fingerTable.getClosestPredecessorToKey(id, key);
                            if (next != null) {
                                if (next.getID() == id) {
                                    String tempMsg = "US " + ip + " " + port;
                                    connector.send(tempMsg, messageList[2], Integer.parseInt(messageList[3]));

                                    Node tempPredecessor = new Node(null, messageList[2], Integer.parseInt(messageList[3]), true);
                                    this.setPredecessor(tempPredecessor);
//                                    gui.echo(this.getPort() + ": my predecessor is : " + this.predecessor.getPort());

                                    //request the finger tabel from my successor
                                    //"RFT <myIP> <myPort>"
                                    /*tempMsg = "RFT " + this.getIp() + " " + this.getPort();
                                socketConnector.send(tempMsg, incomingIP, Integer.parseInt(messageList[3]));
                                gui.echo(this.getPort() + ": request finger table : (" + tempMsg + ")");*/
                                } else {
                                    connector.send(message, next.getIp(), next.getPort());
                                }
                            } else {
                                System.err.println("Couldn't find a succosser for " + message);
                            }
                        }
                    }
                    break;

                case "US": //update succesor
//                    echo("message received: (" + messageList[0] + ")");

                    Node tempSuccessor = new Node(null, messageList[1], Integer.parseInt(messageList[2]), true);
                    this.setSuccessor(tempSuccessor);
//                    gui.echo(this.getPort() + ": my successor is : " + this.successor.getPort());
                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(1000);
                                String tempRFTMsg = "RFT " + ip + " " + port;
                                connector.send(tempRFTMsg, incomingIP, Integer.parseInt(messageList[2]));
//                                gui.echo(port + ": request finger table : (" + tempRFTMsg + ")");
                            } catch (InterruptedException ex) {
                                ex.printStackTrace();
                            }

                        }
                    }.start();

                    break;
                case "RFT": //request finger table
                    echo("message received: (" + messageList[0] + ")");

                    //send finger table
                    //UFT <ip_1> <port_1> <ip_2> <port_2> <ip_3> <port_3>.....
                    if (this.fingerTable.getFingerEntries()[0] == null) {
                        for (int i = 0; i < MAX_FINGERS; i++) {
                            this.fingerTable.updateEntry(i, this);
                            gui.UpdateFingerTable(i, this);
                        }
                    }

                    String tempMsg = "UFT "; //update finger table
                    for (int i = 0; i < MAX_FINGERS; i++) {
                        Node entry = fingerTable.getEntryByIndex(i);
                        tempMsg += entry.getIp() + " " + entry.getPort() + " ";
                    }
                    connector.send(tempMsg, messageList[1], Integer.parseInt(messageList[2]));
//                    gui.echo(this.getPort() + ": sending finger table to : " + messageList[2] + " (" + tempMsg + ")");
                    break;
                case "UFT"://update finger table
                    echo("message received: (" + messageList[0] + ")");

                    int i = 0;
                    Node temp = new Node(null, messageList[(2 * i) + 1], Integer.parseInt(messageList[(2 * i) + 2]), true);

                    for (; i < MAX_FINGERS; i++) {

                        fingerTable.updateEntry(i, temp);
                        gui.UpdateFingerTable(i, temp);
                    }
                    stabilizer.start();
                    fingerFixer.start();
                    predecessorCheckor.start();
                    break;
                case "NOTIFY_S":    // notify succoessor

                    Node tempPredecessor = new Node(null, messageList[1], Integer.parseInt(messageList[2]), this.getBSip(), this.getBSport(), null, false);
                    if (predecessor == null) {
                        this.setPredecessor(tempPredecessor);
//                        gui.echo("NOTIFY_S: Update predecessor of " + id + " to " + tempPredecessor.getID());
                    } else if (predecessor.getID() > this.id) {
                        if ((predecessor.getID() < tempPredecessor.getID() && tempPredecessor.getID() < MAX_NODES)
                                || (0 <= tempPredecessor.getID() && tempPredecessor.getID() < this.id)) {
//                            gui.echo("NOTIFY_S: Update predecessor of " + id + " from  " + predecessor.getID() + " to " + tempPredecessor.getID());
                            this.setPredecessor(tempPredecessor);
                        }
                    } else if (predecessor.getID() < tempPredecessor.getID() && tempPredecessor.getID() < this.id) {
//                        gui.echo("NOTIFY_S: Update predecessor of " + id + " from  " + predecessor.getID() + " to " + tempPredecessor.getID());
                        this.setPredecessor(tempPredecessor);
                    }
                    break;

                case "GET_PRED":    // get predecessor request from Stabilizer
                    String rep = "GET_PRED_OK ";
                    if (this.predecessor != null) {
                        rep += this.predecessor.getIp() + " " + this.predecessor.getPort();
                    } else {
                        rep += "NULL";
                    }
                    redirectMessage(rep, new Node("", messageList[1], Integer.parseInt(messageList[2]), BSip, BSport, null, false));
                    break;

                case "GET_PRED_OK":     // respond from get predecessor request
                    if (!messageList[1].equals("NULL")) {
                        Node newPred = new Node("", messageList[1], Integer.parseInt(messageList[2]), BSip, BSport, null, false);
                        stabilizer.setNewPredessor(newPred);
                    } else {
                        stabilizer.setNullPredecessor(true);
                    }
                    stabilizer.interrupt();
                    break;

                case "FIND_S":   // find successor message from findSuccosser
                    String originIP = messageList[3];
                    int originPort = Integer.parseInt(messageList[4]);
                    if (originIP.equals(ip) && originPort == port) {
                        int fingerIndex = Integer.parseInt(messageList[1]);
                        fingerTable.updateEntry(fingerIndex, this);
                        this.gui.UpdateFingerTable(fingerIndex, fingerTable.getNodeAt(fingerIndex));
                        System.out.println("FixFinger: Update finger " + fingerIndex + " of " + id + " to  " + fingerTable.getNodeAt(fingerIndex).getID());
                        fingerFixer.setWaitingForReply(fingerIndex, false);
                        fingerFixer.setLasFindSIssue(fingerIndex, System.currentTimeMillis());
                    } else {
                        Node succosser = findSuccessorOf(Integer.parseInt(messageList[1]), Integer.parseInt(messageList[2]), originIP, originPort, false);
                        if (succosser != null) {
                            String response = "FIND_S_OK " + messageList[1] + " " + succosser.getIp() + " " + succosser.getPort();
                            redirectMessage(response, new Node("", messageList[3], Integer.parseInt(messageList[4]), BSip, BSport, null, false));
                        }
                    }
                    break;

                case "FIND_S_OK":   // reply to findSuccessor
                    int fingerIndex = Integer.parseInt(messageList[1]);
                    fingerTable.updateEntry(fingerIndex, new Node(null, messageList[2], Integer.parseInt(messageList[3]), BSip, BSport, null, false));
                    this.gui.UpdateFingerTable(fingerIndex, fingerTable.getNodeAt(fingerIndex));
                    System.out.println("FixFinger: Update finger " + fingerIndex + " of " + id + " to  " + fingerTable.getNodeAt(fingerIndex).getID());
                    fingerFixer.setWaitingForReply(fingerIndex, false);
                    fingerFixer.setLasFindSIssue(fingerIndex, System.currentTimeMillis());
                    break;

                case "HB":      // heartbeat
//                    gui.echo("HB recieved. Sending HB_OK");
                    redirectMessage("HB_OK", new Node("", messageList[1], Integer.parseInt(messageList[2]), BSip, BSport, null, false));
                    break;

                case "HB_OK":
//                    gui.echo("HB_OK received");
                    predecessorCheckor.setPredecessorHBOK(true);
                    break;

                case "SER":
                    gui.echo("Received: " + message);
                    String tempIP = messageList[1];
                    int TempPort = Integer.parseInt(messageList[2]);
                    String searchString = message.split("@")[1];
                    gui.echo("Search String: " + searchString);
                    //check whether I have the file.
                    if (files.contains(searchString)) {
                        gui.echo("file exists");
                        //notify the user
                        String searchQuery = "FOUND_FILE" + " " + this.ip + " " + this.port + " " + this.username + " @" + searchString;
//                        int length = searchQuery.length() + 4;
//
//                        searchQuery = String.join("", Collections.nCopies(4 - (Integer.toString(length).length()), "0")) + Integer.toString(length) + searchQuery;

                        connector.send(searchQuery, tempIP, TempPort);
                    } else if (this.searchMetaData(searchString) != null) {
                        Node receiver = this.searchMetaData(searchString);
                        gui.echo("Sending message: " + message + " (to " + receiver.getIp() + ":" + receiver.getPort() + ") - found this node in meta data");
                        connector.send(message, receiver.getIp(), receiver.getPort());
                    } //else do this
                    else {
                        gui.echo("file not exist");
                        Node receiver = null;
                        int hashKey = getHash(searchString);
                        receiver = fingerTable.getNode(hashKey);
                        if (receiver == null) {
                            Node tempReceiver = fingerTable.getClosestPredecessorToKey(hashKey);
                            if (tempReceiver != null) {
                                int tempReceiverID = getHash(tempReceiver.getIp() + tempReceiver.getPort());
                                if (tempReceiverID > this.id) {//otherwise this could send to it self
                                    receiver = tempReceiver;
                                }
                            }
                        }
                        if (receiver == null) {
                            Node tempReceiver = this.successor;
                            int tempReceiverID = getHash(tempReceiver.getIp() + tempReceiver.getPort());
                            if (tempReceiverID <= hashKey && tempReceiverID != getHash(tempIP + TempPort)) {
                                System.out.println("tempReceiverID " + tempReceiverID);
                                System.out.println("DestinationID " + getHash(tempIP + TempPort));
                                receiver = tempReceiver;
                            }
                        }

                        if (receiver != null) {
                            gui.echo("Sending message: " + message + " (to " + receiver.getIp() + ":" + receiver.getPort() + ")");
                            connector.send(message, receiver.getIp(), receiver.getPort());
                        } else {
                            gui.updateDisplay("No receiver found");
                        }
                    }
                    break;
                case "FOUND_FILE":
                    gui.echo(message);
                    String resultIP = messageList[1];
                    int resultPort = Integer.parseInt(messageList[2]);
                    String resultUserName = messageList[3];
                    String resultSearchText = message.split("@")[1];
                    foundFile(resultSearchText, new Node(resultUserName, resultIP, resultPort, null, 55555, null, false));
                    break;

                case "REGMD":
//                    gui.echo(message);
                    String ownerIP = messageList[1];
                    int ownerPort = Integer.parseInt(messageList[2]);
                    String ownerName = messageList[3];
                    String fileName = message.split("@")[1];
                    int hashKey = getHash(fileName);
                    insertFileMetadata(fileName, new Node(ownerName, ownerIP, ownerPort, null, 55555, null, false));

                    Node receiver = fingerTable.getNode(hashKey);
                    if (receiver == null) {
                        receiver = fingerTable.getClosestPredecessorToKey(hashKey);
                    }
//                    if (receiver == null) {
//                        receiver = this.successor;
//                    }
                    if (receiver != null) {
                        connector.send(message, receiver.getIp(), receiver.getPort());
                    } else {
                        gui.echo("No receiver found for metadata distribution");
                    }

                    break;
                default:
                    break;
            }

            if ("REGOK".equals(messageList[1])) {
                //echo("message received: (" + messageList[1] + ")");

                switch (messageList[2]) {
                    case "0":
                        gui.echo("This is the first node.\n");
                        for (int i = 0; i < MAX_FINGERS; i++) {
                            this.fingerTable.updateEntry(i, this);
                            gui.UpdateFingerTable(i, this);
                        }
                        stabilizer.start();
                        fingerFixer.start();
//                        predecessorCheckor.start();
                        break;

                    case "1": {
                        gui.echo("This is the second node.\n");
                        SimpleNeighbor firstNeighbor = new SimpleNeighbor(messageList[3], Integer.parseInt(messageList[4]));
                        neighborList.add(firstNeighbor);
                        joinNetwork();
                        break;
                    }

                    case "2": {
                        gui.echo("This is the third or later node.\n");
                        SimpleNeighbor firstNeighbor = new SimpleNeighbor(messageList[3], Integer.parseInt(messageList[4]));
                        neighborList.add(firstNeighbor);
                        SimpleNeighbor secondNeighbor = new SimpleNeighbor(messageList[5], Integer.parseInt(messageList[6]));
                        neighborList.add(secondNeighbor);
                        joinNetwork();
                        break;
                    }

                    case "9999":
                        gui.echo(" Failed, there is some error in the command.");
                        break;

                    case "9998":
                        gui.echo("Failed, already registered to you, unregister first.");
                        break;

                    case "9997":
                        gui.echo("Failed, registered to another user, try a different IP and port.");
                        break;

                    case "9996":
                        gui.echo("Failed, can’t register. Bootstrap Server full.");
                        break;

                    default:
                        break;
                }
            } else if ("UNROK".equals(messageList[1])) {
                //("message received: (" + messageList[1] + ")");
                switch (messageList[2]) {
                    case "0":
                        gui.echo("Successfully unregistered.\n");
                        this.connector.stop(); //stop listning, equivelent to leave the network
                        stabilizer.stop();
                        fingerFixer.stop();
                        //predecessorCheckor.stop();
                        break;

                    case "9999":
                        gui.echo("Error while unregistering. IP and port may not be in the registry or command is incorrect.");
                        break;
                    default:
                        gui.echo("Some error while unregistering.");
                        break;
                }
            }
        }
    }

    private int getHash(String text) {
        return Math.abs(text.hashCode()) % MAX_NODES;
    }

    /* 
       ask node n to find the successor of key
     */
    public Node findSuccessorOf(int finger, int key, String originIP, int originPort, boolean reqFromFingerFixer) {
        if (this.successor != null) {
            int successorID = this.successor.getID();
            // this node and successor are in opposite side of 0
            if (successorID < this.id) {
                if ((this.id < key && key < MAX_NODES) || (0 <= key && key <= successorID)) {
                    return this.successor;
                }
            } else if (this.id < key && key <= successorID) // normal successor
            {
                return this.successor;
            } else {
                Node nextNode = this.fingerTable.getClosestPredecessorToKey(id, key);
                if (nextNode != null && nextNode.getID() != this.id) {
                    String message = "FIND_S " + finger + " " + key + " " + originIP + " " + originPort;
                    if (reqFromFingerFixer) {
                        this.fingerFixer.setWaitingForReply(finger, true);
                        this.fingerFixer.setLasFindSIssue(finger, System.currentTimeMillis());
                    }
                    redirectMessage(message, nextNode);
                }
                return null;
            }
        }
        return null;
    }

    public void echo(String output) {
        if (gui != null) {
            gui.echo("The message received is: " + output + "\n");
        }
    }

}
