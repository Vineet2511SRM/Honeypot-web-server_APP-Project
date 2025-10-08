import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.sql.*;

/**
 * Handles individual client connections, detects attacks, and logs events.
 */
public class ConnectionHandler implements Runnable {

    private final Socket clientSocket;
    private final Configuration config;

    public ConnectionHandler(Socket socket, Configuration config) {
        this.clientSocket = socket;
        this.config = config;
    }

    private boolean isValidLogin(String username, String password) {
        String dbUrl = "jdbc:sqlite:DBstuff.db";
        String sql = "SELECT * FROM users WHERE username='" + username + "' AND password='" + password + "'"; // Main vulnerability point
        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next(); 
        } catch (SQLException e) {
            JsonLogger.log("DB_ERROR", "Database error during login.", "error", e.getMessage());
            return false;
        }
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

            // parse request method and path
            String[] parts = requestLine.split(" ");
            String method = parts.length > 0 ? parts[0] : "";
            String path = parts.length > 1 ? parts[1] : "/";

            // route GET /login to the login page, keep existing POST handling
            if ("POST".equalsIgnoreCase(method)) {
                handlePostRequest(in, out, clientIp);
            } else if ("GET".equalsIgnoreCase(method) && "/login".equals(path)) {
                HtmlResponder.serveLoginPage(out, config.getServerBanner(), "");
            } else {
                HtmlResponder.serveIndexPage(out, config.getServerBanner());
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
            HtmlResponder.serveFakeLogsPage(out, config.getServerBanner(), username, "DBstuff.db");
            System.out.println("SQL Injection attempt detected from " + clientIp);
        } 
        else {
            if (isValidLogin(username, password)) {
                JsonLogger.log("VALID_LOGIN", "Successful login with valid credentials.", 
                    "clientIp", clientIp, 
                    "userAgent", userAgent, 
                    "username", username);
                HtmlResponder.serveSuccessPage(out, config.getServerBanner(), username);
            } else {
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
    
    //Checks an input string for common SQL injection patterns.

    private boolean isPotentialSqlInjection(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        // A list of common, simple SQL injection patterns/keywords to detect.
        // This is not exhaustive but covers many basic attempts.
        String[] patterns = {
            "' OR '1'='1'", "'OR 1=1", "' OR 'x'='x'",
            "'--", "' #", "'; --", "';#",
            "UNION SELECT", "DROP TABLE", "INSERT INTO", "SELECT * FROM", "SELECT", "UPDATE", "DELETE"
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

