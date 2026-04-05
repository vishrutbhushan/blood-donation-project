package com.hemo.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.Request;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findBySearchUserUserId(Long userId);
}