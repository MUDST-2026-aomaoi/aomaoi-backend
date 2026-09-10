package com.aomaoi.backend.service;

import com.aomaoi.backend.dto.WorkerRequestDTO;
import com.aomaoi.backend.entity.User;
import com.aomaoi.backend.entity.Worker;
import com.aomaoi.backend.repository.UserRepository;
import com.aomaoi.backend.repository.WorkerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkerService {

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Worker> getAllWorkers() {
        return workerRepository.findAll();
    }

    public List<Worker> getActiveWorkers() {
        return workerRepository.findByStatus("active");
    }

    public Worker getWorkerById(Long id) {
        return workerRepository.findById(id).orElseThrow(() -> new RuntimeException("Worker not found"));
    }

    @Transactional
    public Worker addWorker(WorkerRequestDTO dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getTempPassword()));
        user.setRole("worker");
        user.setIsFirstLogin(true);
        user = userRepository.save(user);

        Worker worker = new Worker();
        worker.setFullName(dto.getFullName());
        worker.setNickname(dto.getNickname());
        worker.setPhone(dto.getPhone());
        worker.setAvatar(dto.getAvatar());
        worker.setStatus("pending");
        worker.setUser(user);

        return workerRepository.save(worker);
    }

    @Transactional
    public Worker updateWorker(Long id, WorkerRequestDTO dto) {
        Worker worker = getWorkerById(id);
        worker.setFullName(dto.getFullName());
        worker.setNickname(dto.getNickname());
        worker.setPhone(dto.getPhone());
        worker.setAvatar(dto.getAvatar());

        if (dto.getUsername() != null && !dto.getUsername().equals(worker.getUser().getUsername())) {
            if (userRepository.existsByUsername(dto.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            worker.getUser().setUsername(dto.getUsername());
            userRepository.save(worker.getUser());
        }

        return workerRepository.save(worker);
    }

    @Transactional
    public void deleteWorker(Long id) {
        Worker worker = getWorkerById(id);
        worker.setStatus("inactive");
        workerRepository.save(worker);
    }
}
