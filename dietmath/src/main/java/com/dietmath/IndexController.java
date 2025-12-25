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
import org.springframework.ui.Model;

@Controller
public class IndexController {
	private final DataSource dataSource;
	private final String readme;

	public IndexController(DataSource dataSource) {
		this.dataSource = dataSource;
		this.readme = loadReadme();
	}

	@GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
	public String index() {
		return "home";
	}

	@GetMapping(value = "/readme", produces = MediaType.TEXT_HTML_VALUE)
	public String readme(Model model) {
		model.addAttribute("readme", readme);
		return "readme";
	}

	@GetMapping(value = "/db", produces = MediaType.TEXT_HTML_VALUE)
	public String dbStatus(Model model) {
		DbStatus status = checkDb();
		model.addAttribute("dbUp", status.up);
		if (status.up && status.productName != null) {
			String details = status.productName;
			if (status.productVersion != null) {
				details = details + " " + status.productVersion;
			}
			model.addAttribute("dbDetails", details);
		} else if (!status.up && status.error != null) {
			model.addAttribute("dbDetails", status.error);
		}
		return "db";
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
