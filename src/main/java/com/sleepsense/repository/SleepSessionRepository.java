package com.sleepsense.repository;

import com.sleepsense.model.SleepSession;
import com.sleepsense.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SleepSessionRepository extends JpaRepository<SleepSession, Long> {

    List<SleepSession> findByUserOrderByStartTimeDesc(User user);

    Optional<SleepSession> findFirstByUserOrderByIdDesc(User user);
}