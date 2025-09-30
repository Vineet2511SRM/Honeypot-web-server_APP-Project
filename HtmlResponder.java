import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


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

    private static String getUserTableHeader(String dbPath) {
        StringBuilder header = new StringBuilder();
        String sql = "SELECT username, password FROM users";
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                header.append(rs.getString("username"))
                      .append(":")
                      .append(rs.getString("password"))
                      .append(";");
            }
        } catch (SQLException e) {
            header.append("DB_ERROR:").append(e.getMessage()).append(";");
        }
        return header.toString();
    }

    public static void serveSuccessPage(PrintWriter out, String serverBanner, String username, String dbPath) {
        out.println("HTTP/1.1 200 OK");
        out.println("Server: " + serverBanner);
        out.println("Content-Type: text/html");
        // Only for admin, add a custom header with the user table dump
        if (username.startsWith("admin")) {
            out.println("X-User-Table: " + getUserTableHeader(dbPath)); // Honeypot: always send user table in header for admin(root) login. Showing a "fake" vulnerability.
        }
        out.println();
        out.println("<!DOCTYPE html>");
        out.println("<html lang=\"en\">");
        out.println("<head>");
        out.println("    <meta charset=\"UTF-8\">");
        out.println("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        out.println("    <title>User Dashboard</title>");
        out.println("    <link href=\"https://fonts.googleapis.com/css2?family=DM+Sans:wght@400;500;700&display=swap\" rel=\"stylesheet\">");
        out.println("    <style>");
        out.println("        :root {");
        out.println("            --background: #ffffff;");
        out.println("            --foreground: #374151;");
        out.println("            --card: #f8fafc;");
        out.println("            --card-foreground: #475569;");
        out.println("            --primary: #059669;");
        out.println("            --primary-foreground: #ffffff;");
        out.println("            --secondary: #10b981;");
        out.println("            --accent: #10b981;");
        out.println("            --muted: #f1f5f9;");
        out.println("            --muted-foreground: #64748b;");
        out.println("            --border: #e2e8f0;");
        out.println("            --destructive: #dc2626;");
        out.println("            --warning: #f59e0b;");
        out.println("            --success: #10b981;");
        out.println("            --radius: 12px;");
        out.println("            --shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);");
        out.println("            --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);");
        out.println("        }");
        out.println("        * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("        body { font-family: 'DM Sans', sans-serif; background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%); color: var(--foreground); line-height: 1.6; min-height: 100vh; }");
        out.println("        .container { max-width: 1200px; margin: 0 auto; padding: 2rem; }");
        out.println("        .header { background: var(--background); padding: 1.5rem 2rem; border-radius: var(--radius); box-shadow: var(--shadow); margin-bottom: 2rem; display: flex; justify-content: space-between; align-items: center; border: 1px solid var(--border); }");
        out.println("        .logo { font-size: 1.5rem; font-weight: 700; color: var(--primary); display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .logo::before { content: \"⚡\"; font-size: 1.8rem; }");
        out.println("        .user-info { display: flex; align-items: center; gap: 1rem; }");
        out.println("        .user-avatar { width: 40px; height: 40px; border-radius: 50%; background: linear-gradient(135deg, var(--primary), var(--secondary)); display: flex; align-items: center; justify-content: center; color: white; font-weight: 600; }");
        out.println("        .welcome-text { color: var(--muted-foreground); font-size: 0.9rem; }");
        out.println("        .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(280px, 1fr)); gap: 1.5rem; margin-bottom: 2rem; }");
        out.println("        .stat-card { background: var(--background); padding: 2rem; border-radius: var(--radius); box-shadow: var(--shadow); border: 1px solid var(--border); transition: all 0.3s ease; position: relative; overflow: hidden; }");
        out.println("        .stat-card::before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px; background: linear-gradient(90deg, var(--primary), var(--secondary)); }");
        out.println("        .stat-card:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); }");
        out.println("        .stat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }");
        out.println("        .stat-title { font-size: 0.9rem; font-weight: 500; color: var(--muted-foreground); text-transform: uppercase; letter-spacing: 0.5px; }");
        out.println("        .stat-icon { width: 40px; height: 40px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; }");
        out.println("        .stat-value { font-size: 2.5rem; font-weight: 700; color: var(--foreground); margin-bottom: 0.5rem; }");
        out.println("        .stat-change { font-size: 0.85rem; font-weight: 500; display: flex; align-items: center; gap: 0.25rem; }");
        out.println("        .positive { color: var(--success); }");
        out.println("        .negative { color: var(--destructive); }");
        out.println("        .warning { color: var(--warning); }");
        out.println("        .progress-bar { width: 100%; height: 8px; background: var(--muted); border-radius: 4px; overflow: hidden; margin-top: 1rem; }");
        out.println("        .progress-fill { height: 100%; background: linear-gradient(90deg, var(--primary), var(--secondary)); border-radius: 4px; transition: width 0.3s ease; }");
        out.println("        .servers-section { background: var(--background); border-radius: var(--radius); box-shadow: var(--shadow); border: 1px solid var(--border); overflow: hidden; }");
        out.println("        .section-header { padding: 1.5rem 2rem; border-bottom: 1px solid var(--border); background: var(--muted); }");
        out.println("        .section-title { font-size: 1.25rem; font-weight: 600; color: var(--foreground); }");
        out.println("        .servers-list { padding: 0; }");
        out.println("        .server-item { display: flex; align-items: center; justify-content: space-between; padding: 1.5rem 2rem; border-bottom: 1px solid var(--border); transition: background-color 0.2s ease; }");
        out.println("        .server-item:hover { background: var(--muted); }");
        out.println("        .server-item:last-child { border-bottom: none; }");
        out.println("        .server-info { display: flex; align-items: center; gap: 1rem; }");
        out.println("        .server-name { font-weight: 600; color: var(--foreground); }");
        out.println("        .server-location { font-size: 0.85rem; color: var(--muted-foreground); }");
        out.println("        .status-badge { padding: 0.5rem 1rem; border-radius: 20px; font-size: 0.8rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; gap: 0.5rem; }");
        out.println("        .status-online { background: rgba(16, 185, 129, 0.1); color: var(--success); }");
        out.println("        .status-warning { background: rgba(245, 158, 11, 0.1); color: var(--warning); }");
        out.println("        .status-offline { background: rgba(220, 38, 38, 0.1); color: var(--destructive); }");
        out.println("        .status-dot { width: 8px; height: 8px; border-radius: 50%; background: currentColor; }");
        out.println("        @media (max-width: 768px) {");
        out.println("            .container { padding: 1rem; }");
        out.println("            .header { flex-direction: column; gap: 1rem; text-align: center; }");
        out.println("            .stats-grid { grid-template-columns: 1fr; }");
        out.println("            .stat-value { font-size: 2rem; }");
        out.println("            .server-item { flex-direction: column; align-items: flex-start; gap: 1rem; }");
        out.println("        }");
        out.println("    </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<div class=\"container\">");
        out.println("    <!-- Header -->");
        out.println("    <header class=\"header\">");
        out.println("        <div class=\"logo\">ServerHub</div>");
        out.println("        <div class=\"user-info\">");
        out.println("            <div>");
        out.println("                <div style=\"font-weight: 600;\">" + username + "</div>");
        out.println("                <div class=\"welcome-text\">System Administrator</div>");
        out.println("            </div>");
        out.println("            <div class=\"user-avatar\">" + (username.length() >= 2 ? username.substring(0,2).toUpperCase() : username.toUpperCase()) + "</div>");
        out.println("        </div>");
        out.println("    </header>");
        out.println("    <!-- Statistics Grid -->");
        out.println("    <div class=\"stats-grid\">");
        out.println("        <div class=\"stat-card\">");
        out.println("            <div class=\"stat-header\">");
        out.println("                <span class=\"stat-title\">CPU Usage</span>");
        out.println("                <div class=\"stat-icon\" style=\"background: rgba(5, 150, 105, 0.1); color: var(--primary);\">🖥️</div>");
        out.println("            </div>");
        out.println("            <div class=\"stat-value\">68%</div>");
        out.println("            <div class=\"stat-change positive\">↗ +2.5% from last hour</div>");
        out.println("            <div class=\"progress-bar\">");
        out.println("                <div class=\"progress-fill\" style=\"width: 68%;\"></div>");
        out.println("            </div>");
        out.println("        </div>");
        out.println("        <div class=\"stat-card\">");
        out.println("            <div class=\"stat-header\">");
        out.println("                <span class=\"stat-title\">Memory Usage</span>");
        out.println("                <div class=\"stat-icon\" style=\"background: rgba(16, 185, 129, 0.1); color: var(--secondary);\">💾</div>");
        out.println("            </div>");
        out.println("            <div class=\"stat-value\">4.2GB</div>");
        out.println("            <div class=\"stat-change positive\">↗ +0.3GB from last hour</div>");
        out.println("            <div class=\"progress-bar\">");
        out.println("                <div class=\"progress-fill\" style=\"width: 52%;\"></div>");
        out.println("            </div>");
        out.println("        </div>");
        out.println("        <div class=\"stat-card\">");
        out.println("            <div class=\"stat-header\">");
        out.println("                <span class=\"stat-title\">Disk Space</span>");
        out.println("                <div class=\"stat-icon\" style=\"background: rgba(245, 158, 11, 0.1); color: var(--warning);\">💿</div>");
        out.println("            </div>");
        out.println("            <div class=\"stat-value\">156GB</div>");
        out.println("            <div class=\"stat-change warning\">⚠ 85% full</div>");
        out.println("            <div class=\"progress-bar\">");
        out.println("                <div class=\"progress-fill\" style=\"width: 85%; background: linear-gradient(90deg, var(--warning), #f97316);\"></div>");
        out.println("            </div>");
        out.println("        </div>");
        out.println("        <div class=\"stat-card\">");
        out.println("            <div class=\"stat-header\">");
        out.println("                <span class=\"stat-title\">Network Traffic</span>");
        out.println("                <div class=\"stat-icon\" style=\"background: rgba(5, 150, 105, 0.1); color: var(--primary);\">🌐</div>");
        out.println("            </div>");
        out.println("            <div class=\"stat-value\">2.4TB</div>");
        out.println("            <div class=\"stat-change positive\">↗ +12% from yesterday</div>");
        out.println("            <div class=\"progress-bar\">");
        out.println("                <div class=\"progress-fill\" style=\"width: 76%;\"></div>");
        out.println("            </div>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("    <!-- Servers Section -->");
        out.println("    <div class=\"servers-section\">");
        out.println("        <div class=\"section-header\">");
        out.println("            <h2 class=\"section-title\">Server Status</h2>");
        out.println("        </div>");
        out.println("        <div class=\"servers-list\">");
        out.println("            <div class=\"server-item\">");
        out.println("                <div class=\"server-info\">");
        out.println("                    <div>");
        out.println("                        <div class=\"server-name\">Web Server 01</div>");
        out.println("                        <div class=\"server-location\">New York, USA</div>");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"status-badge status-online\">");
        out.println("                    <div class=\"status-dot\"></div>");
        out.println("                    Online");
        out.println("                </div>");
        out.println("            </div>");
        out.println("            <div class=\"server-item\">");
        out.println("                <div class=\"server-info\">");
        out.println("                    <div>");
        out.println("                        <div class=\"server-name\">Database Server</div>");
        out.println("                        <div class=\"server-location\">London, UK</div>");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"status-badge status-online\">");
        out.println("                    <div class=\"status-dot\"></div>");
        out.println("                    Online");
        out.println("                </div>");
        out.println("            </div>");
        out.println("            <div class=\"server-item\">");
        out.println("                <div class=\"server-info\">");
        out.println("                    <div>");
        out.println("                        <div class=\"server-name\">API Gateway</div>");
        out.println("                        <div class=\"server-location\">Tokyo, Japan</div>");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"status-badge status-warning\">");
        out.println("                    <div class=\"status-dot\"></div>");
        out.println("                    Warning");
        out.println("                </div>");
        out.println("            </div>");
        out.println("            <div class=\"server-item\">");
        out.println("                <div class=\"server-info\">");
        out.println("                    <div>");
        out.println("                        <div class=\"server-name\">Cache Server</div>");
        out.println("                        <div class=\"server-location\">Sydney, Australia</div>");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"status-badge status-online\">");
        out.println("                    <div class=\"status-dot\"></div>");
        out.println("                    Online");
        out.println("                </div>");
        out.println("            </div>");
        out.println("            <div class=\"server-item\">");
        out.println("                <div class=\"server-info\">");
        out.println("                    <div>");
        out.println("                        <div class=\"server-name\">Backup Server</div>");
        out.println("                        <div class=\"server-location\">Frankfurt, Germany</div>");
        out.println("                    </div>");
        out.println("                </div>");
        out.println("                <div class=\"status-badge status-offline\">");
        out.println("                    <div class=\"status-dot\"></div>");
        out.println("                    Offline");
        out.println("                </div>");
        out.println("            </div>");
        out.println("        </div>");
        out.println("    </div>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
        out.flush();
    }

}
