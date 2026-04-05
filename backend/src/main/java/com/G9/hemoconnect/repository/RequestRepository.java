package com.G9.hemoconnect.repository;

import com.G9.hemoconnect.entity.Request;
import com.G9.hemoconnect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findByUserOrderByCreatedAtDesc(User user);
    Optional<Request> findByReqCode(String reqCode);
}
