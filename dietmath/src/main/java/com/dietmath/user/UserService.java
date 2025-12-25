package com.dietmath.user;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.dietmath.user.dto.RegisterRequest;
import com.dietmath.user.dto.RegisterResult;

@Service
public class UserService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = new BCryptPasswordEncoder();
	}

	public RegisterResult register(RegisterRequest request) {
		String username = normalizeUsername(request.username());
		if (username.isEmpty()) {
			return new RegisterResult(false, "Username is required.");
		}
		if (userRepository.existsByUsername(username)) {
			return new RegisterResult(false, "Username already exists.");
		}
		String password = request.password();
		if (password == null || password.isBlank()) {
			return new RegisterResult(false, "Password is required.");
		}
		User user = new User(username, passwordEncoder.encode(password));
		try {
			userRepository.save(user);
		} catch (DataIntegrityViolationException ex) {
			return new RegisterResult(false, "Username already exists.");
		}
		return new RegisterResult(true, "Registration successful.");
	}

	public User authenticate(String username, String password) {
		String normalized = normalizeUsername(username);
		if (normalized.isEmpty() || password == null || password.isBlank()) {
			return null;
		}
		return userRepository.findByUsername(normalized)
			.filter(user -> passwordEncoder.matches(password, user.getPasswordHash()))
			.orElse(null);
	}

	public User findById(Long userId) {
		return userRepository.findById(userId).orElse(null);
	}

	private static String normalizeUsername(String username) {
		if (username == null) {
			return "";
		}
		return username.trim();
	}
}
