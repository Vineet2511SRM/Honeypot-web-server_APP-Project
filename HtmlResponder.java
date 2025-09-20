import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;


public class HtmlResponder {

    public static void serveLoginPage(PrintWriter out, String serverBanner, String errorMessage) {
        String errorHtml = "";
        if (errorMessage != null && !errorMessage.isEmpty()) {
            errorHtml = "<p style='color: #d9534f; text-align: center;'>" + errorMessage + "</p>";
        }

        out.println("HTTP/1.1 200 OK");
        out.println("Server: " + serverBanner);
        out.println("Content-Type: text/html");
        out.println();
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>System Login</title>");
        out.println("<style>");
        out.println("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f4f7f6; color: #333; display: flex; justify-content: center; align-items: center; height: 100vh; margin: 0; }");
        out.println(".login-container { background-color: #fff; padding: 40px; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); width: 100%; max-width: 350px; }");
        out.println("h2 { text-align: center; color: #2c3e50; margin-bottom: 25px; }");
        out.println("input[type='text'], input[type='password'] { width: 100%; padding: 12px; margin-bottom: 15px; border: 1px solid #ccc; border-radius: 4px; box-sizing: border-box; }");
        out.println("button { width: 100%; background-color: #3498db; color: white; padding: 12px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; transition: background-color 0.3s; }");
        out.println("button:hover { background-color: #2980b9; }");
        out.println("</style>");
        out.println("</head><body>");
        out.println("<div class='login-container'>");
        out.println("<h2>System Login</h2>");
        out.println(errorHtml);
        out.println("<form method='POST' action='/login'>");
        out.println("<input type='text' name='username' placeholder='Username' required>");
        out.println("<input type='password' name='password' placeholder='Password' required>");
        out.println("<button type='submit'>Login</button>");
        out.println("</form>");
        out.println("</div>");
        out.println("</body></html>");
        out.flush();
    }

    /**
     * Serves a fake "Access Granted" page with dummy log data to trick attackers.
     */
    public static void serveFakeLogsPage(PrintWriter out, String serverBanner) {
        out.println("HTTP/1.1 200 OK");
        out.println("Server: " + serverBanner);
        out.println("Content-Type: text/html");
        out.println();
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>Access Granted - System Logs</title>");
        out.println("<style>");
        out.println("body { font-family: 'Courier New', monospace; background-color: #0d1117; color: #c9d1d9; padding: 20px; }");
        out.println("h1 { color: #58a6ff; border-bottom: 1px solid #30363d; padding-bottom: 10px; }");
        out.println("pre { background-color: #161b22; border: 1px solid #30363d; border-radius: 6px; padding: 15px; white-space: pre-wrap; word-wrap: break-word; }");
        out.println(".log-entry { margin-bottom: 5px; }");
        out.println(".log-error { color: #f85149; }");
        out.println(".log-success { color: #56d364; }");
        out.println("</style>");
        out.println("</head><body>");
        out.println("<h1>[CLASSIFIED] System Access Logs</h1>");
        out.println("<pre>");
        out.println("<span class='log-success'>[2025-09-20 13:50:01] SUCCESS: User 'root' logged in from 10.0.1.5</span>");
        out.println("<span class='log-entry'>[2025-09-20 13:50:45] INFO: Database connection 'prod_db' established.</span>");
        out.println("<span class='log-error'>[2025-09-20 13:51:12] ERROR: Failed to write to /var/log/sys.log. Permission denied.</span>");
        out.println("<span class='log-entry'>[2025-09-20 13:52:03] INFO: User 'admin' performed action: 'USER_LIST'</span>");
        out.println("<span class='log-success'>[2025-09-20 13:53:10] SUCCESS: User 'system' ran cron job 'backup_db.sh'</span>");
        out.println("</pre>");
        out.println("</body></html>");
        out.flush();
    }

    /**
     * Serves a simple "Welcome" dashboard for successfully authenticated users.
     */
    public static void serveSuccessPage(PrintWriter out, String serverBanner, String username) {
        out.println("HTTP/1.1 200 OK");
        out.println("Server: " + serverBanner);
        out.println("Content-Type: text/html");
        out.println();
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>User Dashboard</title>");
        out.println("<style>");
        out.println("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f4f7f6; color: #333; margin: 0; }");
        out.println(".header { background-color: #2c3e50; color: white; padding: 20px; text-align: center; }");
        out.println(".container { padding: 30px; }");
        out.println(".card { background-color: white; border-radius: 8px; box-shadow: 0 4px 15px rgba(0,0,0,0.1); padding: 20px; margin-top: 20px; }");
        out.println("h1, h2 { margin: 0; }");
        out.println("</style>");
        out.println("</head><body>");
        out.println("<div class='header'><h1>User Dashboard</h1></div>");
        out.println("<div class='container'>");
        out.println("<h2>Welcome back, " + username + "!</h2>");
        out.println("<div class='card'><p>System Status: All systems operational.</p></div>");
        out.println("</div>");
        out.println("</body></html>");
        out.flush();
    }

}
