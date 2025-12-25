package com.dietmath.web;

import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.dietmath.user.UserService;
import com.dietmath.user.dto.RegisterRequest;
import com.dietmath.user.dto.RegisterResult;

import jakarta.validation.Valid;

@Controller
public class RegistrationController {
	private final UserService userService;

	public RegistrationController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping(value = "/register", produces = MediaType.TEXT_HTML_VALUE)
	public String form(Model model) {
		model.addAttribute("message", "");
		model.addAttribute("status", "");
		model.addAttribute("username", "");
		return "register";
	}

	@PostMapping(value = "/register", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
		produces = MediaType.TEXT_HTML_VALUE)
	public String submit(@Valid @ModelAttribute RegisterRequest request, BindingResult bindingResult,
		Model model) {
		if (bindingResult.hasErrors()) {
			String message = bindingResult.getFieldErrors().stream()
				.map(error -> error.getField() + ": " + error.getDefaultMessage())
				.collect(Collectors.joining("; "));
			model.addAttribute("message", message);
			model.addAttribute("status", "error");
			model.addAttribute("username", request.username());
			return "register";
		}
		RegisterResult result = userService.register(request);
		if (result.success()) {
			return "redirect:/login?registered=1";
		}
		model.addAttribute("message", result.message());
		model.addAttribute("status", "error");
		model.addAttribute("username", request.username());
		return "register";
	}
}
