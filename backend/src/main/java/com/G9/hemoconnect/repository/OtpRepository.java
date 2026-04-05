package com.G9.hemoconnect.repository;

import com.G9.hemoconnect.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findTopByPhoneOrderByCreatedAtDesc(String phone);
}
