package com.dietmath.user;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_weights")
public class UserWeight {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, precision = 6, scale = 2)
	private BigDecimal weight;

	@Column(name = "goal_weight", precision = 6, scale = 2)
	private BigDecimal goalWeight;

	@Column(name = "goal_date")
	private LocalDate goalDate;

	@Column(name = "calorie_strategy", nullable = false, length = 16)
	@Enumerated(EnumType.STRING)
	private CalorieStrategy calorieStrategy;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	protected UserWeight() {
	}

	public UserWeight(Long userId, BigDecimal weight, BigDecimal goalWeight, LocalDate goalDate,
		CalorieStrategy calorieStrategy) {
		this.userId = userId;
		this.weight = weight;
		this.goalWeight = goalWeight;
		this.goalDate = goalDate;
		this.calorieStrategy = calorieStrategy;
	}

	@PrePersist
	void onCreate() {
		if (recordedAt == null) {
			recordedAt = Instant.now();
		}
	}

	public Long getUserId() {
		return userId;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public BigDecimal getGoalWeight() {
		return goalWeight;
	}

	public LocalDate getGoalDate() {
		return goalDate;
	}

	public CalorieStrategy getCalorieStrategy() {
		return calorieStrategy;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}
}
