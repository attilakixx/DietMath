package com.dietmath.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dietmath.user.CalorieStrategy;
import com.dietmath.user.User;
import com.dietmath.user.UserRepository;
import com.dietmath.user.UserService;
import com.dietmath.user.UserWeight;
import com.dietmath.user.UserWeightRepository;
import com.dietmath.user.dto.ProfileUpdateRequest;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {
	private static final String SESSION_USER_ID = "userId";
	private static final double CALORIES_PER_KG = 7700.0;
	private static final double MAINTENANCE_PER_KG = 30.0;

	private final UserService userService;
	private final UserRepository userRepository;
	private final UserWeightRepository userWeightRepository;

	public UserController(UserService userService, UserRepository userRepository,
		UserWeightRepository userWeightRepository) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.userWeightRepository = userWeightRepository;
	}

	@GetMapping(value = "/user", produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public ResponseEntity<String> page(@RequestParam(name = "message", required = false) String message,
		@RequestParam(name = "edit", required = false) String edit,
		HttpSession session) {
		Long userId = getUserId(session);
		if (userId == null) {
			return redirectToLogin();
		}
		User user = userService.findById(userId);
		if (user == null) {
			session.invalidate();
			return redirectToLogin();
		}
		Optional<UserWeight> latestWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
		Optional<UserWeight> fixedBaseWeight = userWeightRepository
			.findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(userId, CalorieStrategy.FIXED);
		boolean editWeights = isEditRequested(edit);
		String html = renderPage(user, latestWeight.orElse(null), fixedBaseWeight.orElse(null),
			normalizeMessage(message), "", editWeights);
		return ResponseEntity.ok(html);
	}

	@PostMapping(value = "/user", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
		produces = MediaType.TEXT_HTML_VALUE)
	@ResponseBody
	public ResponseEntity<String> update(@ModelAttribute ProfileUpdateRequest request, BindingResult bindingResult,
		HttpSession session) {
		Long userId = getUserId(session);
		if (userId == null) {
			return redirectToLogin();
		}
		User user = userService.findById(userId);
		if (user == null) {
			session.invalidate();
			return redirectToLogin();
		}

		List<String> errors = new ArrayList<>();
		if (bindingResult.hasErrors()) {
			bindingResult.getAllErrors().forEach(error -> errors.add(error.getDefaultMessage()));
		}

		if (request.getHeight() != null && request.getHeight() <= 0) {
			errors.add("Height must be greater than 0.");
		}
		if (request.getWeight() != null && request.getWeight().compareTo(BigDecimal.ZERO) <= 0) {
			errors.add("Weight must be greater than 0.");
		}
		if (request.getGoalWeight() != null && request.getGoalWeight().compareTo(BigDecimal.ZERO) <= 0) {
			errors.add("Goal weight must be greater than 0.");
		}

		boolean hasWeightData = request.getWeight() != null
			|| request.getGoalWeight() != null
			|| request.getGoalDate() != null
			|| request.getCalorieStrategy() != null;
		boolean editWeights = hasWeightData;
		if (hasWeightData && request.getWeight() == null) {
			errors.add("Weight is required when setting goals or strategy.");
		}

		if (!errors.isEmpty()) {
			Optional<UserWeight> latestWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
			Optional<UserWeight> fixedBaseWeight = userWeightRepository
				.findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(userId, CalorieStrategy.FIXED);
			String html = renderPage(user, latestWeight.orElse(null), fixedBaseWeight.orElse(null),
				"", join(errors), editWeights);
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(html);
		}

		String message = "";
		boolean changed = false;
		if (request.getBirthDate() != null) {
			if (user.getBirthDate() == null) {
				user.setBirthDate(request.getBirthDate());
				changed = true;
			} else if (!user.getBirthDate().equals(request.getBirthDate())) {
				message = "Birth date is immutable and was not changed.";
			}
		}
		if (request.getHeight() != null) {
			user.setHeight(request.getHeight());
			changed = true;
		}
		if (changed) {
			userRepository.save(user);
		}

		boolean weightSaved = false;
		if (request.getWeight() != null) {
			CalorieStrategy strategy = request.getCalorieStrategy();
			if (strategy == null) {
				strategy = CalorieStrategy.DYNAMIC;
			}
			UserWeight weight = new UserWeight(userId, request.getWeight(), request.getGoalWeight(),
				request.getGoalDate(), strategy);
			userWeightRepository.save(weight);
			weightSaved = true;
		}
		if (message.isEmpty() && (changed || weightSaved)) {
			message = "Saved.";
		}

		Optional<UserWeight> latestWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
		Optional<UserWeight> fixedBaseWeight = userWeightRepository
			.findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(userId, CalorieStrategy.FIXED);
		String html = renderPage(user, latestWeight.orElse(null), fixedBaseWeight.orElse(null), message, "", false);
		return ResponseEntity.ok(html);
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
	}

	private ResponseEntity<String> redirectToLogin() {
		return ResponseEntity.status(HttpStatus.FOUND)
			.header("Location", "/login")
			.build();
	}

	private static Long getUserId(HttpSession session) {
		Object value = session.getAttribute(SESSION_USER_ID);
		if (value instanceof Long) {
			return (Long) value;
		}
		if (value instanceof Integer) {
			return ((Integer) value).longValue();
		}
		return null;
	}

	private static String renderPage(User user, UserWeight latestWeight, UserWeight fixedBaseWeight,
		String message, String errorMessage, boolean editWeights) {
		String safeMessage = normalizeMessage(message);
		String safeError = normalizeMessage(errorMessage);
		boolean showWeightForm = editWeights || latestWeight == null;
		StringBuilder html = new StringBuilder();
		html.append("<!doctype html>");
		html.append("<html><head>");
		html.append("<meta charset=\"utf-8\">");
		html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
		html.append("<title>DietMath - User</title>");
		html.append("<style>");
		html.append("body{font-family:ui-monospace,Menlo,Consolas,monospace;margin:24px;color:#111;background:#fafafa;}");
		html.append(".grid{display:grid;gap:16px;grid-template-columns:repeat(auto-fit,minmax(280px,1fr));}");
		html.append(".card{padding:16px;border:1px solid #ddd;border-radius:8px;background:#fff;}");
		html.append("label{display:block;margin:10px 0 4px;}");
		html.append("input,select{width:100%;padding:8px;border:1px solid #bbb;border-radius:6px;}");
		html.append("button{margin-top:12px;padding:10px 16px;border:0;border-radius:6px;background:#111;color:#fff;}");
		html.append(".button-link{display:inline-block;margin-top:12px;padding:10px 16px;border-radius:6px;");
		html.append("background:#111;color:#fff;text-decoration:none;}");
		html.append(".message{margin-bottom:12px;padding:10px 12px;border-radius:6px;}");
		html.append(".success{background:#e7f7ed;border:1px solid #86d19a;}");
		html.append(".error{background:#fdeaea;border:1px solid #f0a5a5;}");
		html.append(".metric{font-size:20px;font-weight:600;}");
		html.append("a{color:#111;}");
		html.append("a.button-link{color:#fff;}");
		html.append("</style>");
		html.append("</head><body>");
		html.append("<div class=\"card\">");
		html.append("<div style=\"display:flex;justify-content:space-between;align-items:center;\">");
		html.append("<h1>Welcome, ");
		html.append(escapeHtml(user.getUsername()));
		html.append("</h1>");
		html.append("<form method=\"post\" action=\"/logout\">");
		html.append("<button type=\"submit\">Logout</button>");
		html.append("</form>");
		html.append("</div>");
		if (!safeMessage.isEmpty()) {
			html.append("<div class=\"message success\">");
			html.append(escapeHtml(safeMessage));
			html.append("</div>");
		}
		if (!safeError.isEmpty()) {
			html.append("<div class=\"message error\">");
			html.append(escapeHtml(safeError));
			html.append("</div>");
		}
		html.append("</div>");

		html.append("<div class=\"grid\">");
		html.append("<div class=\"card\">");
		html.append("<h2>Profile</h2>");
		html.append("<form method=\"post\" action=\"/user\">");
		html.append("<label for=\"birthDate\">Birth date</label>");
		html.append("<input id=\"birthDate\" name=\"birthDate\" type=\"date\" value=\"");
		html.append(user.getBirthDate() != null ? user.getBirthDate() : "");
		html.append("\">");
		html.append("<label for=\"height\">Height (cm)</label>");
		html.append("<input id=\"height\" name=\"height\" type=\"number\" min=\"1\" value=\"");
		html.append(user.getHeight() != null ? user.getHeight() : "");
		html.append("\">");

		html.append("<h3>Weight entry</h3>");
		if (latestWeight != null) {
			html.append("<div>");
			html.append("<p><strong>Latest weight:</strong> ");
			html.append(escapeHtml(formatWeight(latestWeight.getWeight())));
			html.append(" kg</p>");
			html.append("<p><strong>Goal weight:</strong> ");
			html.append(escapeHtml(formatWeight(latestWeight.getGoalWeight())));
			html.append(" kg</p>");
			html.append("<p><strong>Goal date:</strong> ");
			html.append(escapeHtml(formatDate(latestWeight.getGoalDate())));
			html.append("</p>");
			html.append("<p><strong>Strategy:</strong> ");
			html.append(escapeHtml(formatStrategy(latestWeight.getCalorieStrategy())));
			html.append("</p>");
			html.append("<p><strong>Recorded at:</strong> ");
			html.append(escapeHtml(formatInstant(latestWeight.getRecordedAt())));
			html.append("</p>");
			html.append("</div>");
		}
		if (!showWeightForm && latestWeight != null) {
			html.append("<a class=\"button-link\" href=\"/user?edit=1\">Modify</a>");
		}
		if (showWeightForm) {
			String weightValue = latestWeight != null ? formatWeight(latestWeight.getWeight()) : "";
			String goalWeightValue = latestWeight != null ? formatWeight(latestWeight.getGoalWeight()) : "";
			String goalDateValue = latestWeight != null ? formatDate(latestWeight.getGoalDate()) : "";
			CalorieStrategy strategyValue = latestWeight != null ? latestWeight.getCalorieStrategy() : null;

			html.append("<label for=\"weight\">Weight (kg)</label>");
			html.append("<input id=\"weight\" name=\"weight\" type=\"number\" step=\"0.1\" min=\"1\" value=\"");
			html.append(escapeHtml(weightValue));
			html.append("\">");
			html.append("<label for=\"goalWeight\">Goal weight (kg)</label>");
			html.append("<input id=\"goalWeight\" name=\"goalWeight\" type=\"number\" step=\"0.1\" min=\"1\" value=\"");
			html.append(escapeHtml(goalWeightValue));
			html.append("\">");
			html.append("<label for=\"goalDate\">Goal date</label>");
			html.append("<input id=\"goalDate\" name=\"goalDate\" type=\"date\" value=\"");
			html.append(escapeHtml(goalDateValue));
			html.append("\">");
			html.append("<label for=\"calorieStrategy\">Calorie strategy</label>");
			html.append("<select id=\"calorieStrategy\" name=\"calorieStrategy\">");
			html.append("<option value=\"\">Select...</option>");
			html.append("<option value=\"DYNAMIC\"");
			if (strategyValue == CalorieStrategy.DYNAMIC) {
				html.append(" selected");
			}
			html.append(">Dynamic</option>");
			html.append("<option value=\"FIXED\"");
			if (strategyValue == CalorieStrategy.FIXED) {
				html.append(" selected");
			}
			html.append(">Fixed</option>");
			html.append("</select>");
			if (latestWeight != null) {
				html.append("<p>This creates a new weight entry.</p>");
			}
		}
		html.append("<button type=\"submit\">Save</button>");
		html.append("</form>");
		html.append("</div>");

		html.append("<div class=\"card\">");
		html.append("<h2>BMI</h2>");
		String bmiText = calculateBmiText(user, latestWeight);
		html.append("<div class=\"metric\">");
		html.append(escapeHtml(bmiText));
		html.append("</div>");
		html.append("</div>");

		html.append("<div class=\"card\">");
		html.append("<h2>Daily calories</h2>");
		String calorieText = calculateCaloriesText(latestWeight, fixedBaseWeight);
		html.append("<div class=\"metric\">");
		html.append(escapeHtml(calorieText));
		html.append("</div>");
		html.append("<p>Dynamic uses the latest weight entry. Fixed uses the first entry marked fixed.</p>");
		html.append("</div>");
		html.append("</div>");

		html.append("</body></html>");
		return html.toString();
	}

	private static String calculateBmiText(User user, UserWeight latestWeight) {
		if (user.getHeight() == null || latestWeight == null) {
			return "Add height and weight to compute BMI.";
		}
		double heightMeters = user.getHeight() / 100.0;
		if (heightMeters <= 0) {
			return "Height must be greater than 0.";
		}
		double weight = latestWeight.getWeight().doubleValue();
		double bmi = weight / (heightMeters * heightMeters);
		return "BMI: " + roundToOneDecimal(bmi);
	}

	private static String calculateCaloriesText(UserWeight latestWeight, UserWeight fixedBaseWeight) {
		if (latestWeight == null) {
			return "Add a weight entry to see daily calories.";
		}
		CalorieStrategy strategy = latestWeight.getCalorieStrategy();
		UserWeight base = strategy == CalorieStrategy.FIXED && fixedBaseWeight != null
			? fixedBaseWeight
			: latestWeight;
		double weight = base.getWeight().doubleValue();
		double maintenance = weight * MAINTENANCE_PER_KG;

		LocalDate goalDate = base.getGoalDate();
		BigDecimal goalWeightValue = base.getGoalWeight();
		if (goalDate == null || goalWeightValue == null) {
			return "Maintenance: " + roundToWhole(maintenance) + " kcal (no goal set)";
		}
		long days = ChronoUnit.DAYS.between(LocalDate.now(), goalDate);
		if (days <= 0) {
			return "Maintenance: " + roundToWhole(maintenance) + " kcal (goal date passed)";
		}
		double deltaKg = weight - goalWeightValue.doubleValue();
		double dailyDeficit = (deltaKg * CALORIES_PER_KG) / days;
		double recommended = maintenance - dailyDeficit;
		String label = strategy == CalorieStrategy.FIXED ? "Fixed" : "Dynamic";
		return label + " target: " + roundToWhole(recommended) + " kcal";
	}

	private static String roundToOneDecimal(double value) {
		return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).toString();
	}

	private static String roundToWhole(double value) {
		return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toString();
	}

	private static String join(List<String> errors) {
		return String.join("; ", errors);
	}

	private static String normalizeMessage(String message) {
		return message == null ? "" : message;
	}

	private static boolean isEditRequested(String edit) {
		if (edit == null) {
			return false;
		}
		return "1".equals(edit) || "true".equalsIgnoreCase(edit) || "yes".equalsIgnoreCase(edit);
	}

	private static String formatWeight(BigDecimal weight) {
		if (weight == null) {
			return "n/a";
		}
		return weight.stripTrailingZeros().toPlainString();
	}

	private static String formatDate(LocalDate date) {
		return date == null ? "n/a" : date.toString();
	}

	private static String formatInstant(java.time.Instant instant) {
		return instant == null ? "n/a" : instant.toString();
	}

	private static String formatStrategy(CalorieStrategy strategy) {
		if (strategy == null) {
			return "n/a";
		}
		return strategy == CalorieStrategy.FIXED ? "Fixed" : "Dynamic";
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
