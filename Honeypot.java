import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Honeypot {

    public static void main(String[] args) {
        Configuration config = new Configuration("config.properties");
        JsonLogger.setLogFile(config.getLogFile());

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC driver not found!");
            return;
        }

        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadPoolSize());
        
        System.out.println("Honeypot Web Server starting on port " + config.getServerPort());
        JsonLogger.log("SERVER_START", "Honeypot server process started.", "port", String.valueOf(config.getServerPort()));

        try (ServerSocket serverSocket = new ServerSocket(config.getServerPort())) {
            System.out.println("Listening for connections...");
            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    executor.submit(new ConnectionHandler(clientSocket, config));
                } catch (IOException e) {
                    JsonLogger.log("ACCEPT_ERROR", "Error accepting client connection.", "error", e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start server on port " + config.getServerPort() + ": " + e.getMessage());
            JsonLogger.log("STARTUP_FAILURE", "Could not start server.", "error", e.getMessage());
        } finally {
            executor.shutdown();
            JsonLogger.log("SERVER_SHUTDOWN", "Honeypot server process stopped.");
        }
    }
}

