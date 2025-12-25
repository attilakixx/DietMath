package com.dietmath.web;

import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dietmath.user.User;
import com.dietmath.user.UserService;
import com.dietmath.user.dto.LoginRequest;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class LoginController {
	private static final String SESSION_USER_ID = "userId";
	private static final String SESSION_USERNAME = "username";

	private final UserService userService;

	public LoginController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping(value = "/login", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public String form(@RequestParam(name = "registered", required = false) String registered) {
		String message = registered != null ? "Registration successful. Please log in." : "";
		String status = registered != null ? "success" : "";
		return renderForm(status, message, "");
	}

	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
		produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public ResponseEntity<String> submit(@Valid @ModelAttribute LoginRequest request, BindingResult bindingResult,
		HttpSession session) {
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
			return ResponseEntity.badRequest().body(renderForm("error", message, request.username()));
		}
		User user = userService.authenticate(request.username(), request.password());
		if (user == null) {
			return ResponseEntity.badRequest().body(renderForm("error", "Invalid username or password.",
				request.username()));
		}
		session.setAttribute(SESSION_USER_ID, user.getId());
		session.setAttribute(SESSION_USERNAME, user.getUsername());
		return ResponseEntity.status(302).header("Location", "/user").build();
	}

	private static String renderForm(String status, String message, String username) {
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>");
		html.append("<html><head>");
		html.append("<meta charset=\"utf-8\">");
		html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		html.append("<title>Login - DietMath</title>");
		html.append("<style>");
		html.append("body{font-family:ui-monospace,Menlo,Consolas,monospace;margin:24px;color:#111;background:#fafafa;}");
		html.append(".card{max-width:520px;padding:20px;border:1px solid #ddd;border-radius:8px;background:#fff;}");
		html.append("label{display:block;margin:12px 0 4px;}");
		html.append("input{width:100%;padding:10px;border:1px solid #bbb;border-radius:6px;}");
		html.append("button{margin-top:16px;padding:10px 16px;border:0;border-radius:6px;background:#111;color:#fff;}");
		html.append(".message{margin-bottom:12px;padding:10px 12px;border-radius:6px;}");
		html.append(".success{background:#e7f7ed;border:1px solid #86d19a;}");
		html.append(".error{background:#fdeaea;border:1px solid #f0a5a5;}");
		html.append("a{color:#111;}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<div class=\"card\">");
		html.append("<h1>Login</h1>");
		if (!message.isEmpty()) {
			html.append("<div class=\"message ");
			html.append(escapeHtml(status));
			html.append("\">");
			html.append(escapeHtml(message));
			html.append("</div>");
		}
		html.append("<form method=\"post\" action=\"/login\">");
		html.append("<label for=\"username\">Username</label>");
		html.append("<input id=\"username\" name=\"username\" type=\"text\" value=\"");
		html.append(escapeHtml(username));
		html.append("\" autocomplete=\"username\" required>");
		html.append("<label for=\"password\">Password</label>");
		html.append("<input id=\"password\" name=\"password\" type=\"password\" autocomplete=\"current-password\" required>");
		html.append("<button type=\"submit\">Login</button>");
		html.append("</form>");
		html.append("<p><a href=\"/register\">Create an account</a></p>");
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
}
