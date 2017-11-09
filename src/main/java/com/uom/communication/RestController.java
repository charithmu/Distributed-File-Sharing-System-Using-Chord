package com.uom.communication;

import com.uom.chord.Node;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestController {

    private final Node myNode;

    public RestController(Node node) {
        this.myNode = node;
    }

    @GET
    @Path("/rest/{msg}")
    public Response routeTable(@PathParam("msg") String msg, @Context HttpServletRequest request) {
        if (msg != null) {
            myNode.handleMessage(msg, request.getRemoteAddr());
            return Response.status(Response.Status.OK).build();
        }
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
}
