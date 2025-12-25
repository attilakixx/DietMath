package com.dietmath;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class IndexController {
	private final DataSource dataSource;
	private final String readme;

	public IndexController(DataSource dataSource) {
		this.dataSource = dataSource;
		this.readme = loadReadme();
	}

	@GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String index() {
		return renderHome();
	}

	@GetMapping(value = "/readme", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String readme() {
		return renderReadme(readme);
	}

	@GetMapping(value = "/db", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String dbStatus() {
		DbStatus status = checkDb();
		return renderDbStatus(status);
	}

	private DbStatus checkDb() {
		try (Connection connection = dataSource.getConnection()) {
			DatabaseMetaData meta = connection.getMetaData();
			return new DbStatus(true, meta.getDatabaseProductName(), meta.getDatabaseProductVersion(), null);
		} catch (SQLException ex) {
			return new DbStatus(false, null, null, ex.getMessage());
		}
	}

	private String loadReadme() {
		ClassPathResource resource = new ClassPathResource("readme.md");
		if (!resource.exists()) {
			return "README not found.";
		}
		try (InputStream input = resource.getInputStream()) {
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return "Failed to load README: " + ex.getMessage();
		}
	}

	private static String renderHome() {
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>");
		html.append("<html><head>");
		html.append("<meta charset=\"utf-8\">");
		html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		html.append("<title>DietMath</title>");
		html.append("<style>");
		html.append("body{font-family:ui-monospace,Menlo,Consolas,monospace;margin:24px;color:#111;background:#fafafa;}");
		html.append(".card{padding:16px;border:1px solid #ddd;border-radius:8px;background:#fff;max-width:720px;}");
		html.append("ul{padding-left:18px;}");
		html.append("a{color:#111;}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<div class=\"card\">");
		html.append("<h1>DietMath</h1>");
		html.append("<p>Self-hosted calorie counter backend.</p>");
		html.append("<ul>");
		html.append("<li><a href=\"/readme\">Project README</a></li>");
		html.append("<li><a href=\"/db\">Database status checker</a></li>");
		html.append("<li><a href=\"/login\">Login</a></li>");
		html.append("<li><a href=\"/register\">Register</a></li>");
		html.append("</ul>");
		html.append("</div>");
		html.append("</body></html>");
		return html.toString();
	}

	private static String renderReadme(String readmeText) {
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>");
		html.append("<html><head>");
		html.append("<meta charset=\"utf-8\">");
		html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		html.append("<title>DietMath - README</title>");
		html.append("<style>");
		html.append("body{font-family:ui-monospace,Menlo,Consolas,monospace;margin:24px;color:#111;background:#fafafa;}");
		html.append("pre{white-space:pre-wrap;line-height:1.4;background:#fff;border:1px solid #ddd;");
		html.append("padding:16px;border-radius:8px;}");
		html.append("a{color:#111;}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<p><a href=\"/\">Back to home</a></p>");
		html.append("<pre>");
		html.append(escapeHtml(readmeText));
		html.append("</pre>");
		html.append("</body></html>");
		return html.toString();
	}

	private static String renderDbStatus(DbStatus status) {
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>");
		html.append("<html><head>");
		html.append("<meta charset=\"utf-8\">");
		html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		html.append("<title>DietMath - DB Status</title>");
		html.append("<style>");
		html.append("body{font-family:ui-monospace,Menlo,Consolas,monospace;margin:24px;color:#111;background:#fafafa;}");
		html.append(".status{padding:12px 16px;border-radius:8px;margin-bottom:16px;}");
		html.append(".up{background:#e7f7ed;border:1px solid #86d19a;}");
		html.append(".down{background:#fdeaea;border:1px solid #f0a5a5;}");
		html.append("a{color:#111;}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<p><a href=\"/\">Back to home</a></p>");
		html.append("<div class=\"status ");
		html.append(status.up ? "up" : "down");
		html.append("\">");
		html.append("<strong>Database: ");
		html.append(status.up ? "UP" : "DOWN");
		html.append("</strong>");
		if (status.up && status.productName != null) {
			html.append(" - ");
			html.append(escapeHtml(status.productName));
			if (status.productVersion != null) {
				html.append(" ");
				html.append(escapeHtml(status.productVersion));
			}
		}
		if (!status.up && status.error != null) {
			html.append("<div>");
			html.append(escapeHtml(status.error));
			html.append("</div>");
		}
		html.append("</div>");
		html.append("</body></html>");
		return html.toString();
	}

	private static String escapeHtml(String input) {
		if (input == null) {
			return "";
		}
		StringBuilder escaped = new StringBuilder(input.length());
		for (int i = 0; i < input.length(); i++) {
			char ch = input.charAt(i);
			switch (ch) {
				case '&' -> escaped.append("&amp;");
				case '<' -> escaped.append("&lt;");
				case '>' -> escaped.append("&gt;");
				case '"' -> escaped.append("&quot;");
				default -> escaped.append(ch);
			}
		}
		return escaped.toString();
	}

	private static final class DbStatus {
		private final boolean up;
		private final String productName;
		private final String productVersion;
		private final String error;

		private DbStatus(boolean up, String productName, String productVersion, String error) {
			this.up = up;
			this.productName = productName;
			this.productVersion = productVersion;
			this.error = error;
		}
	}
}
