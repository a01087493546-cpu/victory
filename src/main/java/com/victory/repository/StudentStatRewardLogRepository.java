package com.victory.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.victory.entity.StudentStatRewardLog;

public interface StudentStatRewardLogRepository
        extends JpaRepository<StudentStatRewardLog, Long> {

    Optional<StudentStatRewardLog> findByStudent_IdAndRewardTypeAndInstanceId(
            Long studentId,
            String rewardType,
            String instanceId);

    List<StudentStatRewardLog> findByStudent_Id(Long studentId);
}
