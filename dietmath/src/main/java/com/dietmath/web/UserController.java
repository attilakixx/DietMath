package com.dietmath.web;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

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
	private static final double BMI_MIN = 15.0;
	private static final double BMI_MAX = 40.0;
	private static final ZoneId USER_ZONE = ZoneId.systemDefault();

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
	public String page(@RequestParam(name = "message", required = false) String message,
		@RequestParam(name = "edit", required = false) String edit,
		HttpSession session, Model model) {
		Long userId = getUserId(session);
		if (userId == null) {
			return "redirect:/login";
		}
		User user = userService.findById(userId);
		if (user == null) {
			session.invalidate();
			return "redirect:/login";
		}
		Optional<UserWeight> latestWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
		Optional<UserWeight> fixedBaseWeight = userWeightRepository
			.findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(userId, CalorieStrategy.FIXED);
		boolean editWeights = isEditRequested(edit);
		populateModel(model, user, latestWeight.orElse(null), fixedBaseWeight.orElse(null),
			editWeights, normalizeMessage(message), "");
		return "user";
	}

	@PostMapping(value = "/user", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
		produces = MediaType.TEXT_HTML_VALUE)
	public String update(@ModelAttribute ProfileUpdateRequest request, BindingResult bindingResult,
		HttpSession session, Model model) {
		Long userId = getUserId(session);
		if (userId == null) {
			return "redirect:/login";
		}
		User user = userService.findById(userId);
		if (user == null) {
			session.invalidate();
			return "redirect:/login";
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
			populateModel(model, user, latestWeight.orElse(null), fixedBaseWeight.orElse(null),
				editWeights, "", join(errors));
			return "user";
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
		populateModel(model, user, latestWeight.orElse(null), fixedBaseWeight.orElse(null),
			false, message, "");
		return "user";
	}

	@PostMapping(value = "/user/weight", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
		produces = MediaType.TEXT_HTML_VALUE)
	public String updateDailyWeight(@RequestParam(name = "dailyWeight", required = false) String dailyWeight,
		HttpSession session, Model model) {
		Long userId = getUserId(session);
		if (userId == null) {
			return "redirect:/login";
		}
		User user = userService.findById(userId);
		if (user == null) {
			session.invalidate();
			return "redirect:/login";
		}

		String error = validateDailyWeight(dailyWeight);
		Optional<UserWeight> latestWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
		Optional<UserWeight> fixedBaseWeight = userWeightRepository
			.findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(userId, CalorieStrategy.FIXED);

		if (!error.isEmpty()) {
			populateModel(model, user, latestWeight.orElse(null), fixedBaseWeight.orElse(null),
				false, "", error);
			return "user";
		}

		BigDecimal weightValue = new BigDecimal(dailyWeight.trim());
		UserWeight previous = latestWeight.orElse(null);
		CalorieStrategy strategy = previous != null ? previous.getCalorieStrategy() : CalorieStrategy.DYNAMIC;
		BigDecimal goalWeight = previous != null ? previous.getGoalWeight() : null;
		LocalDate goalDate = previous != null ? previous.getGoalDate() : null;

		UserWeight entry = new UserWeight(userId, weightValue, goalWeight, goalDate, strategy);
		userWeightRepository.save(entry);

		Optional<UserWeight> updatedWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtDesc(userId);
		Optional<UserWeight> updatedFixed = userWeightRepository
			.findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(userId, CalorieStrategy.FIXED);
		populateModel(model, user, updatedWeight.orElse(null), updatedFixed.orElse(null),
			false, "Daily weight updated.", "");
		return "user";
	}

	@GetMapping(value = "/user/weights", produces = MediaType.TEXT_HTML_VALUE)
	public String weights(@RequestParam(name = "from", required = false)
		@DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
		HttpSession session, Model model) {
		Long userId = getUserId(session);
		if (userId == null) {
			return "redirect:/login";
		}
		User user = userService.findById(userId);
		if (user == null) {
			session.invalidate();
			return "redirect:/login";
		}

		Optional<UserWeight> firstWeight = userWeightRepository.findTopByUserIdOrderByRecordedAtAsc(userId);
		LocalDate defaultFrom = firstWeight
			.map(weight -> LocalDate.ofInstant(weight.getRecordedAt(), USER_ZONE))
			.orElse(LocalDate.now());

		LocalDate today = LocalDate.now();
		LocalDate fromDate = from != null ? from : defaultFrom;
		String note = "";
		if (fromDate.isAfter(today)) {
			fromDate = today;
			note = "From date adjusted to today.";
		}

		Instant start = fromDate.atStartOfDay(USER_ZONE).toInstant();
		Instant end = today.plusDays(1).atStartOfDay(USER_ZONE).toInstant();
		List<UserWeight> weights = userWeightRepository
			.findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(userId, start, end);

		List<WeightPoint> points = new ArrayList<>();
		List<WeightRecord> records = new ArrayList<>();
		for (UserWeight weight : weights) {
			LocalDate date = LocalDate.ofInstant(weight.getRecordedAt(), USER_ZONE);
			String dateIso = date.toString();
			points.add(new WeightPoint(dateIso, weight.getWeight()));
			records.add(new WeightRecord(formatDateDisplay(date), formatWeightDisplay(weight.getWeight())));
		}

		model.addAttribute("username", user.getUsername());
		model.addAttribute("fromDate", fromDate.toString());
		model.addAttribute("toDate", today.toString());
		model.addAttribute("chartPoints", points);
		model.addAttribute("hasPoints", !points.isEmpty());
		model.addAttribute("weightRecords", records);
		model.addAttribute("note", note);
		model.addAttribute("entryCount", points.size());
		return "weights";
	}

	@PostMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/login";
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

	private void populateModel(Model model, User user, UserWeight latestWeight, UserWeight fixedBaseWeight,
		boolean editWeights, String message, String errorMessage) {
		boolean showWeightForm = editWeights || latestWeight == null;
		model.addAttribute("username", user.getUsername());
		model.addAttribute("message", normalizeMessage(message));
		model.addAttribute("errorMessage", normalizeMessage(errorMessage));
		model.addAttribute("birthDate", user.getBirthDate());
		model.addAttribute("height", user.getHeight());
		model.addAttribute("showWeightForm", showWeightForm);
		model.addAttribute("hasWeight", latestWeight != null);
		model.addAttribute("weightDisplay", formatWeightDisplay(latestWeight != null ? latestWeight.getWeight() : null));
		model.addAttribute("goalWeightDisplay",
			formatWeightDisplay(latestWeight != null ? latestWeight.getGoalWeight() : null));
		model.addAttribute("goalDateDisplay",
			formatDateDisplay(latestWeight != null ? latestWeight.getGoalDate() : null));
		model.addAttribute("strategyDisplay",
			formatStrategyDisplay(latestWeight != null ? latestWeight.getCalorieStrategy() : null));
		model.addAttribute("recordedAtDisplay",
			formatInstantDisplay(latestWeight != null ? latestWeight.getRecordedAt() : null));
		model.addAttribute("weightValue", formatWeightValue(latestWeight != null ? latestWeight.getWeight() : null));
		model.addAttribute("goalWeightValue",
			formatWeightValue(latestWeight != null ? latestWeight.getGoalWeight() : null));
		model.addAttribute("goalDateValue",
			formatDateValue(latestWeight != null ? latestWeight.getGoalDate() : null));
		model.addAttribute("strategyValue", latestWeight != null ? latestWeight.getCalorieStrategy() : null);
		Double bmiValue = calculateBmiValue(user, latestWeight);
		if (bmiValue == null) {
			model.addAttribute("bmiHasValue", false);
			model.addAttribute("bmiText", "Add height and weight to compute BMI.");
			model.addAttribute("bmiStatus", "");
			model.addAttribute("bmiDescription", "");
			model.addAttribute("bmiMarkerLeft", "0%");
		} else {
			model.addAttribute("bmiHasValue", true);
			model.addAttribute("bmiText", "BMI: " + roundToOneDecimal(bmiValue));
			model.addAttribute("bmiStatus", bmiStatus(bmiValue));
			model.addAttribute("bmiDescription", bmiDescription(bmiValue));
			model.addAttribute("bmiMarkerLeft", bmiMarkerLeft(bmiValue));
		}
		model.addAttribute("calorieText", calculateCaloriesText(latestWeight, fixedBaseWeight));
	}

	private static Double calculateBmiValue(User user, UserWeight latestWeight) {
		if (user.getHeight() == null || latestWeight == null) {
			return null;
		}
		double heightMeters = user.getHeight() / 100.0;
		if (heightMeters <= 0) {
			return null;
		}
		double weight = latestWeight.getWeight().doubleValue();
		double bmi = weight / (heightMeters * heightMeters);
		return bmi;
	}

	private static String bmiStatus(double bmi) {
		if (bmi < 18.5) {
			return "Underweight";
		}
		if (bmi < 25.0) {
			return "Normal";
		}
		if (bmi < 30.0) {
			return "Overweight";
		}
		return "Obese";
	}

	private static String bmiDescription(double bmi) {
		if (bmi < 18.5) {
			return "Below normal range (18.5-24.9).";
		}
		if (bmi < 25.0) {
			return "Within the normal range (18.5-24.9).";
		}
		if (bmi < 30.0) {
			return "Above normal range (18.5-24.9).";
		}
		return "Well above normal range (18.5-24.9).";
	}

	private static String bmiMarkerLeft(double bmi) {
		double clamped = Math.max(BMI_MIN, Math.min(BMI_MAX, bmi));
		double percent = ((clamped - BMI_MIN) / (BMI_MAX - BMI_MIN)) * 100.0;
		return String.format(Locale.US, "%.1f%%", percent);
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

	private static String formatWeightValue(BigDecimal weight) {
		if (weight == null) {
			return "";
		}
		return weight.stripTrailingZeros().toPlainString();
	}

	private static String formatWeightDisplay(BigDecimal weight) {
		if (weight == null) {
			return "n/a";
		}
		return formatWeightValue(weight);
	}

	private static String formatDateValue(LocalDate date) {
		return date == null ? "" : date.toString();
	}

	private static String formatDateDisplay(LocalDate date) {
		return date == null ? "n/a" : date.toString();
	}

	private static String formatInstantDisplay(Instant instant) {
		return instant == null ? "n/a" : instant.toString();
	}

	private static String formatStrategyDisplay(CalorieStrategy strategy) {
		if (strategy == null) {
			return "n/a";
		}
		return strategy == CalorieStrategy.FIXED ? "Fixed" : "Dynamic";
	}

	private record WeightPoint(String date, BigDecimal weight) {
	}

	private record WeightRecord(String date, String weight) {
	}

	private static String validateDailyWeight(String weight) {
		if (weight == null || weight.isBlank()) {
			return "Daily weight is required.";
		}
		try {
			BigDecimal value = new BigDecimal(weight.trim());
			if (value.compareTo(BigDecimal.ZERO) <= 0) {
				return "Daily weight must be greater than 0.";
			}
		} catch (NumberFormatException ex) {
			return "Daily weight must be a number.";
		}
		return "";
	}

}
