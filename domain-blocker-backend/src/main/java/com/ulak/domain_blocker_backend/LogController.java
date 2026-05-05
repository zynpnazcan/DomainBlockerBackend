package com.ulak.domain_blocker_backend;

import com.ulak.domain_blocker_backend.model.SystemLog;
import com.ulak.domain_blocker_backend.repository.SystemLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*")
public class LogController {

    @Autowired
    private SystemLogRepository systemLogRepository;

    @GetMapping
    public List<SystemLog> getAllLogs() {
        return systemLogRepository.findAllByOrderByTimestampDesc();
    }
}
