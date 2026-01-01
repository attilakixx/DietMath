package com.dietmath.user;

import java.util.Optional;
import java.util.List;
import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserWeightRepository extends JpaRepository<UserWeight, Long> {
	Optional<UserWeight> findTopByUserIdOrderByRecordedAtDesc(Long userId);
	Optional<UserWeight> findTopByUserIdOrderByRecordedAtAsc(Long userId);
	Optional<UserWeight> findTopByUserIdAndCalorieStrategyOrderByRecordedAtAsc(Long userId,
		CalorieStrategy calorieStrategy);
	List<UserWeight> findByUserIdAndRecordedAtBetweenOrderByRecordedAtAsc(Long userId, Instant start, Instant end);
}
