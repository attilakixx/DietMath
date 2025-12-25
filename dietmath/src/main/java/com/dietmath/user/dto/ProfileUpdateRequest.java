package com.dietmath.user.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.dietmath.user.CalorieStrategy;

public class ProfileUpdateRequest {
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate birthDate;

	private Integer height;

	private BigDecimal weight;

	private BigDecimal goalWeight;

	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate goalDate;

	private CalorieStrategy calorieStrategy;

	public LocalDate getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public BigDecimal getWeight() {
		return weight;
	}

	public void setWeight(BigDecimal weight) {
		this.weight = weight;
	}

	public BigDecimal getGoalWeight() {
		return goalWeight;
	}

	public void setGoalWeight(BigDecimal goalWeight) {
		this.goalWeight = goalWeight;
	}

	public LocalDate getGoalDate() {
		return goalDate;
	}

	public void setGoalDate(LocalDate goalDate) {
		this.goalDate = goalDate;
	}

	public CalorieStrategy getCalorieStrategy() {
		return calorieStrategy;
	}

	public void setCalorieStrategy(CalorieStrategy calorieStrategy) {
		this.calorieStrategy = calorieStrategy;
	}
}
