import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles individual client connections, detects attacks, and logs events.
 */
public class ConnectionHandler implements Runnable {

    private final Socket clientSocket;
    private final Configuration config;
    private static final Map<String, String> userDatabase = new HashMap<>();

    // Static block to initialize the fake user database
    static {
        userDatabase.put("admin", "21232f297a57a5a743894a0e4a801fc3"); // MD5 for "admin"
        userDatabase.put("j.doe", "password123");
        userDatabase.put("m.smith", "qwerty");
    }

    public ConnectionHandler(Socket socket, Configuration config) {
        this.clientSocket = socket;
        this.config = config;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        JsonLogger.log("CONNECTION_RECEIVED", "Connection received.", "clientIp", clientIp);

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream())
        ) {
            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            JsonLogger.log("HTTP_REQUEST", "Request received.", "clientIp", clientIp, "requestLine", requestLine);

            // Handle POST requests (form submissions) differently from GET requests
            if (requestLine.startsWith("POST")) {
                handlePostRequest(in, out, clientIp);
            } else {
                HtmlResponder.serveLoginPage(out, config.getServerBanner(), null);
            }

        } catch (IOException e) {
            JsonLogger.log("CONNECTION_ERROR", "Error during connection.", "clientIp", clientIp, "error", e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                JsonLogger.log("SOCKET_CLOSE_ERROR", "Failed to close socket.", "clientIp", clientIp, "error", e.getMessage());
            }
        }
    }
    
    private void handlePostRequest(BufferedReader in, PrintWriter out, String clientIp) throws IOException {
        Map<String, String> headers = readHeaders(in);
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        String formData = new String(body);

        Map<String, String> params = parseFormData(formData);
        String username = params.getOrDefault("username", "");
        String password = params.getOrDefault("password", "");
        String userAgent = headers.getOrDefault("User-Agent", "Unknown");

        // More robust SQL injection detection
        boolean isSqlInjection = isPotentialSqlInjection(username) || isPotentialSqlInjection(password);

        if (isSqlInjection) {
            // If an attack is detected, log it and serve the fake logs
            JsonLogger.log("SQL_INJECTION_DETECTED", "SQL injection attempt detected.", 
                "clientIp", clientIp, 
                "userAgent", userAgent, 
                "username", username, 
                "password", password);
            HtmlResponder.serveFakeLogsPage(out, config.getServerBanner());
        } else {
            // If no SQLi, check for valid credentials
            boolean isValidLogin = userDatabase.containsKey(username) && userDatabase.get(username).equals(password);

            if (isValidLogin) {
                // Log the successful non-admin login and show the "secret" page
                JsonLogger.log("VALID_LOGIN", "Successful login with valid credentials.", 
                    "clientIp", clientIp, 
                    "userAgent", userAgent, 
                    "username", username);
                HtmlResponder.serveSuccessPage(out, config.getServerBanner(), username);
            } else {
                // For all other attempts, log as a failed login and show an error
                JsonLogger.log("LOGIN_ATTEMPT", "Login attempt failed.", 
                    "clientIp", clientIp, 
                    "userAgent", userAgent, 
                    "username", username, 
                    "password", password);
                HtmlResponder.serveLoginPage(out, config.getServerBanner(), "Login Failed: Invalid credentials.");
            }
        }
    }

    private Map<String, String> readHeaders(BufferedReader in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            String[] parts = line.split(": ", 2);
            if (parts.length == 2) {
                headers.put(parts[0], parts[1]);
            }
        }
        return headers;
    }

    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        try {
            for (String param : formData.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    params.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name()), URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()));
                }
            }
        } catch (Exception e) {
            JsonLogger.log("FORM_PARSE_ERROR", "Could not parse form data.", "formData", formData, "error", e.getMessage());
        }
        return params;
    }
    
    /**
     * Checks an input string for common SQL injection patterns.
     * @param input The string to check.
     * @return true if a potential SQL injection pattern is found, false otherwise.
     */
    private boolean isPotentialSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // A list of common, simple SQL injection patterns/keywords to detect.
        // This is not exhaustive but covers many basic attempts.
        String[] patterns = {
            "' OR '1'='1'", "'OR 1=1", "' OR 'x'='x'",
            "'--", "' #", "'; --", "';#",
            "UNION SELECT", "DROP TABLE", "INSERT INTO", "SELECT * FROM"
        };

        // Check for patterns in a case-insensitive way
        String upperInput = input.toUpperCase();
        for (String pattern : patterns) {
            if (upperInput.contains(pattern)) {
                return true;
            }
        }
        return false;
    }
}

