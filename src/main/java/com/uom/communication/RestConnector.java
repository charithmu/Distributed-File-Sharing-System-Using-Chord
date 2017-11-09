package com.uom.communication;

import com.uom.chord.Node;
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

/**
 * This Class provides the implementation of the server side, of each of the
 * nodes.
 *
 * @author Vithusha Aarabhi
 * @author Jayan Vidanapathirana
 */
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
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
