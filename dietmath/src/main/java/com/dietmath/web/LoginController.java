package com.dietmath.web;

import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
	public String form(@RequestParam(name = "registered", required = false) String registered, Model model) {
		String message = registered != null ? "Registration successful. Please log in." : "";
		String status = registered != null ? "success" : "";
		model.addAttribute("message", message);
		model.addAttribute("status", status);
		model.addAttribute("username", "");
		return "login";
	}

	@PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
		produces = MediaType.TEXT_HTML_VALUE)
	public String submit(@Valid @ModelAttribute LoginRequest request, BindingResult bindingResult,
		HttpSession session, Model model) {
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
			model.addAttribute("message", message);
			model.addAttribute("status", "error");
			model.addAttribute("username", request.username());
			return "login";
		}
		User user = userService.authenticate(request.username(), request.password());
		if (user == null) {
			model.addAttribute("message", "Invalid username or password.");
			model.addAttribute("status", "error");
			model.addAttribute("username", request.username());
			return "login";
		}
		session.setAttribute(SESSION_USER_ID, user.getId());
		session.setAttribute(SESSION_USERNAME, user.getUsername());
		return "redirect:/user";
	}
}
