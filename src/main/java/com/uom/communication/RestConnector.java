package com.uom.communication;

import com.uom.chord.Node;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

public class RestConnector implements Connector {

    private boolean started = false;

    private final Node myNode;
    private Server jettyServer;

    public RestConnector(Node myNode) {
        this.myNode = myNode;
    }

    @Override
    public void stop() {
        if (started) {
            started = false;
            try {
                jettyServer.stop();
            } catch (Exception e) {
                System.out.println("Error occurred when stopping the REST server due to : {}" + e.getMessage());
            }
        }
    }

    @Override
    public void listen(int port) {
        if (started) {
            System.out.println("Listener already running");
        } else {
            ResourceConfig config = new ResourceConfig();
            config.register(new RestController(myNode));

            ServletHolder servlet = new ServletHolder(new ServletContainer(config));

            jettyServer = new Server(myNode.getPort());

            ServletContextHandler context = new ServletContextHandler(jettyServer, null);
            context.addServlet(servlet, "/*");
            context.setSessionHandler(new SessionHandler());
            jettyServer.setHandler(context);

            try {
                jettyServer.start();
            } catch (Exception e) {
                System.err.println("Error occurred when starting REST server due to" + e);
                return;
            }

            System.out.println("REST Server started successfully ...");
        }
    }

    @Override
    public void send(String msg, String ip, int port) {
        UriBuilder url = UriBuilder.fromPath("rest")
                .path(msg)
                .scheme("http")
                .host(ip)
                .port(port);
        
        System.out.println(url.toString());

        Client client = JerseyClientBuilder.createClient();

        String response = client.target(url)
                .request(MediaType.APPLICATION_JSON)
                .get(String.class);
        
        if (response.equals(Response.Status.OK.toString())) {
            System.out.println("Message Successfully Sent.");
        }else{
            System.out.println("Message did not delivered.");
        }
    }

    @Override
    public void sendToBS(String message) {
        try {
            DatagramSocket socket = new DatagramSocket();

            byte[] b = message.getBytes();

            DatagramPacket dp = new DatagramPacket(b, b.length, InetAddress.getByName(myNode.getBSip()), myNode.getBSport());
            socket.send(dp);

            //now receive reply
            //buffer to receive incoming data
            byte[] buffer = new byte[65536];
            DatagramPacket repl = new DatagramPacket(buffer, buffer.length);
            socket.receive(repl);

            byte[] data = repl.getData();
            String reply = new String(data, 0, repl.getLength());

            myNode.handleMessage(reply, repl.getAddress().getHostAddress());

        } catch (IOException e) {
            System.err.println("IOException " + e);
        }
    }

}
