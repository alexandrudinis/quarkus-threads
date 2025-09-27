package demo;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserResource {

    @Inject
    UserService userService;

    // 1) NON-BLOCKING (event-loop)
    @GET
    @Path("/non-blocking/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @NonBlocking
    public Uni<UserDTO> nonBlocking(@PathParam("name") String name) {
        return userService.getUserReactive(name);
    }

    // 2) NON-BLOCKING forced on blocking code (event-loop)
    @GET
    @Path("/fake-non-blocking/{name}")
    @NonBlocking
    @Produces(MediaType.APPLICATION_JSON)
    public UserDTO fakeNonBlocking(@PathParam("name") String name) {
        return userService.getUser(name);
    }

    // 3) BLOCKING VIRTUAL THREADS
    @GET
    @Path("/virtual-threads/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @RunOnVirtualThread
    public UserDTO virtualThreads(@PathParam("name") String name) {
        return userService.getUser(name);
    }

    // 4) BLOCKING (worker thread)
    @GET
    @Path("/blocking/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Blocking
    public UserDTO blocking(@PathParam("name") String name) {
        return userService.getUser(name);
    }

    @GET
    @Path("/call-external")
    public Uni<String> externalCall() throws Exception {
       return userService.callExternalService();
    }

}
