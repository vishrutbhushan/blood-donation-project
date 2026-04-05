package com.hemo.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.Request;


public interface RequestRepository extends JpaRepository<Request, Long> {}