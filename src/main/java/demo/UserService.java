package demo;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.time.Duration;

@ApplicationScoped
public class UserService {

    // JDBC/HTTP blocking call
    public UserDTO getUser(String name) {
        Log.infof("Db call on %s", Thread.currentThread());

        try {
            Thread.sleep(5000);
            return new UserDTO(name);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted", e);
        }
    }

    // Reactive I/O (NON-BLOCKING)
    public Uni<UserDTO> getUserReactive(String name) {
        Log.infof("Db call on %s", Thread.currentThread());

        return Uni.createFrom().item(new UserDTO(name))
                .onItem()
                .delayIt()
                .by(Duration.ofMillis(5000));
    }

    public Uni<String> callExternalService() throws Exception {
        Log.infof("Calling external service on %s", Thread.currentThread());

        try (HttpClient client = HttpClient.newHttpClient();) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new java.net.URI("http://localhost:8080/external-call"))
                    .timeout(Duration.ofMillis(1000))
                    .build();
            java.net.http.HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            return Uni.createFrom().item(response.body());
        }
    }
}
