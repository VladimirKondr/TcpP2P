package KilimanJARo.P2P.client;

import KilimanJARo.P2P.client.request.*;
import KilimanJARo.P2P.client.response.*;
import KilimanJARo.P2P.client.tunneling.Tunnel;
import KilimanJARo.P2P.server.requests.AuthRequest;
import KilimanJARo.P2P.server.requests.LogoutRequest;
import KilimanJARo.P2P.server.requests.RegisterRequest;
import KilimanJARo.P2P.server.responses.AuthResponse;
import KilimanJARo.P2P.server.responses.LogoutResponse;
import KilimanJARo.P2P.server.responses.RegisterResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

@RestController
@RequestMapping("/api")
public class ClientServerController {
    private final RestTemplate restTemplate;
    private final SmartProperties central_properties;
    private final SmartProperties properties;
    private String username = "";

    /**
     * This field is for test purposes only and will be removed later and moved to front.
     */
    @Deprecated
    private String password;

    private final Map<String, String> localToPublicIDTubeMap = new HashMap<>();
    private final Map<String, Tunnel> publicIDToLocalTunnels = new HashMap<>();

    @Autowired
    public ClientServerController(@Qualifier("ClientServerRestTemplate") RestTemplate restTemplate, @Qualifier("centralServerProperties") PropertiesFactoryBean centralServerProperties, @Qualifier("serverProperties") PropertiesFactoryBean serverProperties) throws IOException {
        this.restTemplate = restTemplate;
        this.central_properties = new SmartProperties(centralServerProperties.getObject());
        this.properties = new SmartProperties(serverProperties.getObject());
    }

    @GetMapping("/registerWithMainServer")
    public ResponseEntity<RegisterResponse> registerWithMainServer(@RequestParam RegisterRequest request) {
        username = request.name();
        RegisterRequest requestToCentral = new RegisterRequest(username);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");
        RequestEntity<RegisterRequest> requestEntity =
            RequestEntity.post(URI.create(central_properties.getProperty("server.api.register.url")))
            .headers(headers)
            .body(requestToCentral);
        ResponseEntity<RegisterResponse> response = restTemplate.exchange(requestEntity, RegisterResponse.class);
        password = response.getBody().password();

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new RegisterResponse(true, "Server registered successfully", response.getBody().password()));
        } else {
            return ResponseEntity.status(500).body(new RegisterResponse(false, "Server registration failed", null));
        }
    }

    @GetMapping("/authWithMainServer")
    public ResponseEntity<AuthResponse> authWithMainServer(@RequestParam AuthRequest request) {
        String usernameIn = request.name();
        if (!usernameIn.equals(username)) {
            return ResponseEntity.status(403).body(new AuthResponse(false, "Server authentication failed", null));
        }
        AuthRequest requestToCentral = new AuthRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<AuthRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.login.url")))
                .headers(headers)
                .body(requestToCentral);

        ResponseEntity<AuthResponse> response = restTemplate.exchange(requestEntity, AuthResponse.class);
        password = response.getBody().nextPassword();

        if (response.getBody() != null && response.getBody().success()) {
            return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().nextPassword()));
        } else {
            return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
        }
    }

    @Deprecated
    @GetMapping("/authWithMainServerAuto")
    public ResponseEntity<AuthResponse> authWithMainServerAuto() {
        AuthRequest request = new AuthRequest(username, password);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<AuthRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.login.url")))
                .headers(headers)
                .body(request);

        ResponseEntity<AuthResponse> response = restTemplate.exchange(requestEntity, AuthResponse.class);
        password = response.getBody().nextPassword();

        if (response.getBody() != null && response.getBody().success()) {
            return ResponseEntity.ok(new AuthResponse(true, "Server authenticated successfully", response.getBody().nextPassword()));
        } else {
            return ResponseEntity.status(500).body(new AuthResponse(false, "Server authentication failed", null));
        }
    }

    @GetMapping("/logoutFromMainServer")
    public ResponseEntity<LogoutResponse> logoutFromMainServer(@RequestParam LogoutRequest request) {
        String usernameIn = request.username();
        if (!usernameIn.equals(username)) {
            return ResponseEntity.status(403).body(new LogoutResponse(false, "Logout failed due to invalid credentials"));
        }
        LogoutRequest requestToCentral = new LogoutRequest(username);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<LogoutRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.logout.url")))
                .headers(headers)
                .body(requestToCentral);

        ResponseEntity<LogoutResponse> response = restTemplate.exchange(requestEntity, LogoutResponse.class);

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new LogoutResponse(true, "Logged out successfully"));
        } else {
            return ResponseEntity.status(500).body(new LogoutResponse(false, "Logout failed"));
        }
    }

    @PostMapping("/makeTube")
    public ResponseEntity<CreateTunnelResponse> makeTube(@RequestParam CreateTunnelRequest request) {
        String from = request.from();
        String to = request.to();
        String tunnel_id = request.tunnelId();
        boolean readable = false;
        if (from == null && to == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
        }
        if (from == null) {
            from = properties.getProperty("front.port");
            if (from == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
            }
            readable = true;
        }
        if (to == null) {
            to = properties.getProperty("front.port");
            if (to == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
            }
            readable = true;
        }
        Tunnel tunnel = Tunnel.Create(from, to, tunnel_id);
        if (tunnel == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CreateTunnelResponse(false));
        }

        publicIDToLocalTunnels.put(tunnel_id, tunnel);
        return ResponseEntity.ok(new CreateTunnelResponse(true));
    }

    @PostMapping("/closeTube")
    public ResponseEntity<CloseTunnelResponse> closeTube(@RequestParam CloseTunnelRequest request) {
        try {
            String tunnel_id = request.tunnelId();
            publicIDToLocalTunnels.get(tunnel_id).close();
            publicIDToLocalTunnels.remove(tunnel_id);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new CloseTunnelResponse(false, e.getMessage()));
        }
        return ResponseEntity.ok(new CloseTunnelResponse(true, "Tunnel closed successfully"));
    }

    @PostMapping("/requestConnectionIn")
    public ResponseEntity<EstablishConnectionResponse> requestConnectionIn(@RequestParam EstablishConnectionRequest request) {
        ResponseEntity<EstablishConnectionResponse> response = restTemplate.postForEntity(properties.getProperty("front.api.handleConnectionRequest.url"), request, EstablishConnectionResponse.class);

        if (response.getBody() != null && response.getBody().isAllowed()) {
            return ResponseEntity.ok(new EstablishConnectionResponse(true, "Connection allowed"));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new EstablishConnectionResponse(false, "Connection denied"));
        }
    }

    @PostMapping("/establishConnection")
    public void establishedConnection(@RequestBody ConnectionEstablishedRequest request) {
        String url = properties.getProperty("front.api.connectionEstablished.url");
        restTemplate.postForEntity(url, request, Void.class);
    }

    @PostMapping("/requestConnectionOut")
    public ResponseEntity<EstablishConnectionResponse> requestConnectionOut(@RequestParam EstablishConnectionRequest request) {
        EstablishConnectionRequest requestToCentral = new EstablishConnectionRequest(username, request.to());
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");
        RequestEntity<EstablishConnectionRequest> requestEntity =
                RequestEntity.post(URI.create(central_properties.getProperty("server.api.requestConnection.url")))
                        .headers(headers)
                        .body(requestToCentral);
        ResponseEntity<EstablishConnectionResponse> response = restTemplate.exchange(requestEntity, EstablishConnectionResponse.class);
        if (response.getBody() != null && response.getBody().isAllowed()) {
            return ResponseEntity.ok(response.getBody());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new EstablishConnectionResponse(false, "failure"));
    }

    @PostMapping("/requestCloseTube")
    public ResponseEntity<CloseConnectionResponse> requestCloseTube(@RequestParam CloseConnectionRequest request) {
        String usernameIn = request.username();
        CloseConnectionRequest requestToCentral = new CloseConnectionRequest(usernameIn);
        HttpHeaders headers = new HttpHeaders();
        // headers.setBasicAuth("username", "password");
        // headers.set("Content-Type", "application/json");

        RequestEntity<CloseConnectionRequest> requestEntity = RequestEntity
                .post(URI.create(central_properties.getProperty("server.api.closeConnection.url")))
                .headers(headers)
                .body(requestToCentral);

        ResponseEntity<CloseConnectionResponse> response = restTemplate.exchange(requestEntity, CloseConnectionResponse.class);

        if (response.getBody() != null && response.getBody().isSuccess()) {
            return ResponseEntity.ok(new CloseConnectionResponse(true));
        } else {
            return ResponseEntity.status(500).body(new CloseConnectionResponse(false));
        }
    }

    private String generateLocalId() {
        String local_id;
        do {
            local_id = RandomStringGenerator.generateRandomString(10);
        } while (localToPublicIDTubeMap.containsKey(local_id));
        return local_id;
    }

    private static class RandomStringGenerator {
        private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        private static final SecureRandom RANDOM = new SecureRandom();

        public static String generateRandomString(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                int index = RANDOM.nextInt(CHARACTERS.length());
                sb.append(CHARACTERS.charAt(index));
            }
            return sb.toString();
        }
    }
}
