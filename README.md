# Quarkus Blocking vs Non\-Blocking Demo

## 1\. Purpose
Demonstrates how improper blocking operations on the Vert\.x event loop in Quarkus cause stalled requests (app appears frozen) and how to fix it using:
\- Reactive `Uni`
\- `@Blocking` (worker pool)
\- `@RunOnVirtualThread`
\- Proper non\-blocking external HTTP calls

## 2\. Core Idea
Event loop threads must never run long blocking operations (`Thread.sleep`, blocking I/O, synchronous HTTP/JDBC). When they do, all other requests sharing that event loop are delayed. This project shows:
\- Correct reactive delay (non\-blocking)
\- Incorrect forced non\-blocking (blocking on event loop)
\- Safe offloading to worker pool
\- Safe isolation with virtual threads
\- A blocking external HTTP call currently executed on the event loop (problem)
\- How to fix it

## 3\. Endpoints

| Endpoint | Annotation / Thread | Current Behavior |
|----------|---------------------|------------------|
| `/api/non-blocking/{name}` | `@NonBlocking` (event loop) | Reactive delay (OK) |
| `/api/fake-non-blocking/{name}` | `@NonBlocking` (event loop) | Uses `Thread.sleep` (WRONG) blocks event loop |
| `/api/virtual-threads/{name}` | `@RunOnVirtualThread` | Blocking isolated (OK) |
| `/api/blocking/{name}` | `@Blocking` (worker) | Blocking off event loop (OK) |
| `/api/call-external` | (no annotation) event loop | Calls blocking `HttpClient.send` (WRONG) |
| `/external-call` | `@Blocking` | Simulated slow external endpoint (5s sleep) |

Thread name hints:
\- Event loop: `vert.x-eventloop-thread-*`
\- Worker: `executor-thread-*`
\- Virtual: `VirtualThread[#...]`

## 4\. Running the Application

Dev mode:
```bash
./mvnw quarkus:dev
```

Docker image:
```bash
./mvnw package
docker build -f src/main/docker/Dockerfile.jvm -t quarkus-thread-demo .
docker run -p 8080:8080 quarkus-thread-demo
```

## 5\. Baseline Non\-Blocking Behavior
Two concurrent non\-blocking calls do not delay each other:
```bash
time curl localhost:8080/api/non-blocking/A &
time curl localhost:8080/api/non-blocking/B
```
Each finishes ~5s after its own start.

## 6\. Demonstrate Event Loop Blocking
First call blocks event loop; second waits:
```bash
time curl localhost:8080/api/fake-non-blocking/A &
sleep 1
time curl localhost:8080/api/non-blocking/B
```

## 7\. Worker Thread Offloading
Blocking isolated from event loop:
```bash
time curl localhost:8080/api/blocking/A &
sleep 1
time curl localhost:8080/api/non-blocking/B
```
Second starts its own reactive timer immediately.

## 8\. Virtual Threads
Similar isolation using virtual threads:
```bash
time curl localhost:8080/api/virtual-threads/A &
sleep 1
time curl localhost:8080/api/non-blocking/B
```

## 9\. Blocking External Call (Problem)
Currently `/api/call-external` runs on event loop and performs a synchronous HTTP call:
```bash
time curl localhost:8080/api/call-external &
sleep 1
time curl localhost:8080/api/non-blocking/C
```
Second request is delayed: event loop blocked by `HttpClient.send`.

## 10\. Logs: Identifying Issues
Look for lines:
```
Db call on Thread[vert.x-eventloop-thread-0,...]
Calling external service on Thread[vert.x-eventloop-thread-0,...]
```
If a blocking call appears on `vert.x-eventloop-thread-*`, it is incorrect.

## 11\. Current Issues Highlighted
\- `fakeNonBlocking` uses `Thread.sleep` under `@NonBlocking`
\- `callExternalService`:
\- Creates a new `HttpClient` per call
\- Uses blocking `send`
\- Executes on event loop (no annotation)

## 12\. Recommended Fixes

### A\. Fix fake non\-blocking
Options:
```java
// Replace @NonBlocking with one of:
@Blocking
// or
@RunOnVirtualThread
// or convert to:
public Uni<UserDTO> fakeNonBlocking(...) { return getUserReactive(...); }
```

### B\. Make external call non\-blocking (preferred)
Reuse a single client and use `sendAsync`:
```java
java
@ApplicationScoped
public class UserService {
    private final HttpClient client = HttpClient.newHttpClient();

    public Uni<String> callExternalService() {
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/external-call"))
            .build();

        return Uni.createFrom()
            .completionStage(
                client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            )
            .onItem().transform(HttpResponse::body);
    }
}
```

### C\. Or annotate endpoint to offload (simpler, still blocking)
```java
java
@GET
@Path("/call-external")
@Blocking
public String externalCall() throws Exception {
    return userService.callExternalBlocking();
}
```

### D\. Or virtual thread
```java
java
@GET
@Path("/call-external")
@RunOnVirtualThread
public String externalCall() throws Exception {
    return userService.callExternalBlocking();
}
```

### E\. Wrapping blocking call in worker pool
```java
java
public Uni<String> callExternalServiceBlockingWrapped() {
    return Uni.createFrom().item(() -> {
        HttpResponse<String> r = client.send(request, BodyHandlers.ofString());
        return r.body();
    }).runSubscriptionOn(io.smallrye.mutiny.infrastructure.Infrastructure.getDefaultWorkerPool());
}
```

## 13\. Quick Endpoint Samples
```bash
curl localhost:8080/api/non-blocking/John
curl localhost:8080/api/fake-non-blocking/Jane
curl localhost:8080/api/blocking/Bob
curl localhost:8080/api/virtual-threads/Ana
curl localhost:8080/api/call-external
curl localhost:8080/external-call
```

## 14\. Observing Thread Names
Add logging already present; verify:
\- Reactive delay stays on `vert.x-eventloop-thread-*`
\- Blocking work shows `executor-thread-*` or `VirtualThread`
\- No `Thread.sleep` inside event loop


## 16\. Key Takeaways
\- Never block on the event loop
\- Use reactive APIs (`Uni`) or offload (`@Blocking`, `@RunOnVirtualThread`)
\- Reuse resources (single `HttpClient`)
\- Watch thread names to validate correctness

## 17\. Files of Interest
\- `src/main/java/demo/UserResource.java`
\- `src/main/java/demo/UserService.java`
\- `src/main/java/demo/ExternalCallResource.java`
\- `src/main/docker/Dockerfile.jvm`

## 18\. Summary
This project is a concise playground to visualize and measure the impact of blocking calls on Quarkus event loops and to apply correct mitigation patterns.