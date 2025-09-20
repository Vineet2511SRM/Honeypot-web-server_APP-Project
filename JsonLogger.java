import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


class JsonLogger {
    private static String LOG_FILE = "honeypot.json";
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    public static void setLogFile(String logFile) {
        LOG_FILE = logFile;
    }

    public static synchronized void log(String eventType, String message, String... keyValuePairs) {
        try (FileWriter fw = new FileWriter(LOG_FILE, true)) {
            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"timestamp\":\"").append(dtf.format(LocalDateTime.now())).append("\",");
            json.append("\"eventType\":\"").append(escape(eventType)).append("\",");
            json.append("\"message\":\"").append(escape(message)).append("\"");

            if (keyValuePairs.length > 0) {
                for (int i = 0; i < keyValuePairs.length; i += 2) {
                    json.append(",");
                    json.append("\"").append(escape(keyValuePairs[i])).append("\":\"").append(escape(keyValuePairs[i+1])).append("\"");
                }
            }
            json.append("}\n");
            fw.write(json.toString());
        } catch (IOException e) {
            System.err.println("Error: Could not write to log file: " + e.getMessage());
        }
    }
    
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
