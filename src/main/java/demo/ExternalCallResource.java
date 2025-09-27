package demo;

import io.smallrye.common.annotation.Blocking;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/external-call")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ExternalCallResource {
    @GET
    @Blocking
    public String externalCall() {

        try {
            Thread.sleep(5000);
            return "External call completed";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "External call completed";
    }
}
