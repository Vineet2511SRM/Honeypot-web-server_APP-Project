import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


// MAIN CLASS
//It listens for incoming connections on a web port and dispatches them to a thread pool.
public class Honeypot {

    private static final int PORT = 8080; 
    private static final int THREAD_POOL_SIZE = 10;

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        
        System.out.println("Honeypot Web Server starting on port " + PORT);
        Logger.log("Honeypot Web Server started.");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Listening for connections...");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(new ConnectionHandler(clientSocket));
                } catch (IOException e) {
                    String errorMessage = "Error accepting connection: " + e.getMessage();
                    System.err.println(errorMessage);
                    Logger.log(errorMessage);
                }
            }
        } catch (IOException e) {
            String errorMessage = "Could not start server on port " + PORT + ": " + e.getMessage();
            System.err.println(errorMessage);
            Logger.log(errorMessage);
        } finally {
            executor.shutdown();
            Logger.log("Honeypot Web Server stopped.");
        }
    }
}

// Connection Handler
// Handles an HTTP connection, checks for form submissions and potential sql injection.
class ConnectionHandler implements Runnable {

    private final Socket clientSocket;

    private static final Map<String, String> userDatabase = new HashMap<>();
    static {
        userDatabase.put("admin", "21232f297a57a5a743894a0e4a801fc3"); 
        userDatabase.put("j.doe", "password123");
        userDatabase.put("m.smith", "qwerty");
    }

    public ConnectionHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        String clientIp = clientSocket.getInetAddress().getHostAddress();
        String connectionInfo = "Connection received from " + clientIp;
        System.out.println(connectionInfo);
        Logger.log(connectionInfo);

        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream())
        ) {
            String requestLine = in.readLine();
            if (requestLine == null) return;

            Logger.log("Request from " + clientIp + ": " + requestLine);
            System.out.println("Request from " + clientIp + ": " + requestLine);

            if (requestLine.startsWith("POST")) {
                handlePostRequest(in, out, clientIp);
            } else {
                serveLoginPage(out, null);
            }

        } catch (IOException e) {
            String errorMessage = "Error handling connection for " + clientIp + ": " + e.getMessage();
            System.err.println(errorMessage);
            Logger.log(errorMessage);
        } finally {
            try {
                clientSocket.close();
                String disconnectMessage = "Connection with " + clientIp + " closed.";
                System.out.println(disconnectMessage);
                Logger.log(disconnectMessage);
            } catch (IOException e) {
                Logger.log("Error closing socket for " + clientIp + ": " + e.getMessage());
            }
        }
    }
    
    // Check for sql injection
    private void handlePostRequest(BufferedReader in, PrintWriter out, String clientIp) throws IOException {
        Map<String, String> headers = readHeaders(in);
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        char[] body = new char[contentLength];
        in.read(body, 0, contentLength);
        String formData = new String(body);

        Map<String, String> params = parseFormData(formData);
        String username = params.getOrDefault("username", "").trim();
        String password = params.getOrDefault("password", "");

        // --- THE TRAP --- (for explaining to ma'am)
        // This simulates a vulnerable query like: 
        // SELECT * FROM users WHERE username = '[username]' AND password = '[password]'
        // The attacker tries to comment out the password check. (common sql injection technique)
        
        // A more realistic sql injection payload for bypassing login:
        boolean isSqlInjection = username.equalsIgnoreCase("admin' --") || 
                                 username.equalsIgnoreCase("admin'#") || 
                                 username.equalsIgnoreCase("admin' -- ");

        if (isSqlInjection) {
            String userAgent = headers.getOrDefault("User-Agent", "Unknown");
            String successLog = String.format(
                "!!! SQL Injection Detected from %s [Device: %s] | Payload -> user: [%s], pass: [%s]",
                clientIp, userAgent, username, password
            );
            System.out.println(successLog);
            Logger.log(successLog); // Log attacker (example, has to be refined after ma'am's review --if needed--)
            serveFakeLogsPage(out);
        } else {
            // Log a standard failed attempt.
            String logMessage = String.format("Login attempt from %s -> user: [%s], pass: [%s]", clientIp, username, password);
            System.out.println(logMessage);
            Logger.log(logMessage);
            serveLoginPage(out, "Login Failed: Invalid credentials.");
        }
    }

    // Dummy html page for the user login thing. Need to develop the website and add it here.
    private void serveLoginPage(PrintWriter out, String errorMessage) {
        String errorHtml = "";
        if (errorMessage != null) {
            errorHtml = "<p style='color: red;'>" + errorMessage + "</p>";
        }

        String htmlResponse = "<html><head><title>Corporate Login</title></head>" +
                              "<body style='font-family: sans-serif; background-color: #f4f4f4; text-align: center;'>" +
                              "<div style='margin: 50px auto; width: 350px; padding: 20px; background-color: #fff; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1);'>" +
                              "<h2>Corporate Intranet Login</h2>" + errorHtml +
                              "<form method='POST' action='/login'>" +
                              "<p><input type='text' name='username' placeholder='Username' style='width: 90%; padding: 8px;'></p>" +
                              "<p><input type='password' name='password' placeholder='Password' style='width: 90%; padding: 8px;'></p>" +
                              "<p><input type='submit' value='Login' style='width: 95%; padding: 10px; background-color: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer;'></p>" +
                              "</form></div></body></html>";
        
        sendHttpResponse(out, htmlResponse);
    }

    // Dummy access granted thing, needa add easter eggs and stuff we want to over here.
    private void serveFakeLogsPage(PrintWriter out) {
        String htmlResponse = "<html><head><title>Access Granted</title></head>" +
                              "<body style='font-family: monospace; background-color: #111; color: #0f0;'>" +
                              "<h2>Authentication Bypass Successful. Displaying System Logs...</h2>" +
                              "<pre>" +
                              "2023-10-27 10:00:01 INFO: User 'admin' logged in from 192.168.1.10\n" +
                              "2023-10-27 10:05:23 INFO: Service 'DB-CONNECTOR' started successfully.\n" +
                              "2023-10-27 10:10:45 WARN: High CPU usage detected on node 'worker-03'.\n" +
                              "2023-10-27 10:11:12 INFO: SELECT * FROM user_accounts WHERE id = 1;\n" +
                              "2023-10-27 10:15:00 INFO: User 'system' performed backup operation.\n" +
                              "2023-10-27 10:20:30 ERROR: Failed to connect to payment gateway 'Stripe'.\n" +
                              "</pre></body></html>";
        
        sendHttpResponse(out, htmlResponse);
    }

    // Helper for sending a complete 200 OK response
    private void sendHttpResponse(PrintWriter out, String html) {
        out.println("HTTP/1.1 200 OK");
        out.println("Content-Type: text/html");
        out.println("Content-Length: " + html.getBytes(StandardCharsets.UTF_8).length);
        out.println("Server: Apache/2.4.1 (Unix)");
        out.println();
        out.println(html);
        out.flush();
    }
    
    // Helper for reading headers
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

    // For parsing form data into url
    private Map<String, String> parseFormData(String formData) {
        Map<String, String> params = new HashMap<>();
        try {
            for (String param : formData.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2) {
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
                    String value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name());
                    params.put(key, value);
                }
            }
        } catch (Exception e) {
            Logger.log("Error parsing form data: " + e.getMessage());
        }
        return params;
    }
}


// Logger for logging stuff (formatted for windows for now)
class Logger {
    private static final String LOG_FILE = "honeypot_web.log";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static synchronized void log(String message) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            String timestamp = dtf.format(LocalDateTime.now());
            fw.write(timestamp + " | " + message + System.lineSeparator());
        } catch (IOException e) {
            System.err.println("Error: Could not write to log file: " + e.getMessage());
        }
    }
}
