import java.io.FileReader;
import java.io.IOException;

import java.util.Properties;


public class Configuration {
    private Properties properties = new Properties();

    public Configuration(String fileName) {
        try (FileReader reader = new FileReader(fileName)) {
            properties.load(reader);
        } catch (IOException e) {
            System.err.println("FATAL: Could not load configuration file: " + fileName);
        }
    }

    public int getServerPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }

    public int getThreadPoolSize() {
        return Integer.parseInt(properties.getProperty("server.threadpool.size", "10"));
    }

    public String getServerBanner() {
        return properties.getProperty("http.server.banner", "Server");
    }

    public String getLogFile() {
        return properties.getProperty("log.file", "honeypot.json");
    }
}
