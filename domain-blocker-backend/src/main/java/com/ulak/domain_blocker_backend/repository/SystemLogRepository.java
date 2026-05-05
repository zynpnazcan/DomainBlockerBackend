package com.ulak.domain_blocker_backend.repository;

import com.ulak.domain_blocker_backend.model.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findAllByOrderByTimestampDesc();
}