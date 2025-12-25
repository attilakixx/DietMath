package com.dietmath.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWeightRepository extends JpaRepository<UserWeight, Long> {
	Optional<UserWeight> findTopByUserIdOrderByRecordedAtDesc(Long userId);
	Optional<UserWeight> findTopByUserIdOrderByRecordedAtAsc(Long userId);
	Optional<UserWeight> findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(Long userId,
		CalorieStrategy calorieStrategy);
}
