package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WorkerService {

    private final WorkerRepository workerRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public WorkerService(WorkerRepository workerRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.workerRepository = workerRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<Map<String, Object>> getAllWorkers() {
        return workerRepository.findAll().stream()
                .map(this::workerToMap)
                .collect(Collectors.toList());
    }

    public Map<String, Object> addWorker(WorkerRequestDTO dto) {
        // Create login account for the worker if tempPassword is provided
        User user = null;
        if (dto.getTempPassword() != null && !dto.getTempPassword().isEmpty()) {
            user = new User();
            user.setUsername(dto.getUsername());
            user.setPassword(passwordEncoder.encode(dto.getTempPassword()));
            user.setRole("worker");
            user.setIsFirstLogin(true);
            user = userRepository.save(user);
        }

        Worker worker = new Worker();
        worker.setFullName(dto.getFullName());
        worker.setNickname(dto.getNickname());
        worker.setUsername(dto.getUsername());
        worker.setPhone(dto.getPhone());
        worker.setAvatar(dto.getAvatar());
        worker.setStatus("pending");
        worker.setJoinedDate(LocalDate.now());
        worker.setUser(user);

        Worker saved = workerRepository.save(worker);
        return workerToMap(saved);
    }

    public Map<String, Object> updateWorker(Long id, WorkerRequestDTO dto) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found with id: " + id));

        if (dto.getFullName() != null) worker.setFullName(dto.getFullName());
        if (dto.getNickname() != null) worker.setNickname(dto.getNickname());
        if (dto.getUsername() != null) worker.setUsername(dto.getUsername());
        if (dto.getPhone() != null) worker.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) worker.setAvatar(dto.getAvatar());

        Worker saved = workerRepository.save(worker);
        return workerToMap(saved);
    }

    public Map<String, Object> softDeleteWorker(Long id) {
        Worker worker = workerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Worker not found with id: " + id));

        worker.setStatus("inactive");
        Worker saved = workerRepository.save(worker);
        return workerToMap(saved);
    }

    private Map<String, Object> workerToMap(Worker w) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", String.valueOf(w.getId()));
        map.put("fullName", w.getFullName());
        map.put("nickname", w.getNickname());
        map.put("username", w.getUsername());
        map.put("phone", w.getPhone());
        map.put("status", w.getStatus());
        map.put("joinedDate", w.getJoinedDate().toString());
        map.put("avatar", w.getAvatar());
        return map;
    }
}
