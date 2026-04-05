package com.hemo.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.Response;    
import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    List<Response> findByRequestRequestId(Long requestId);

    long countByRequestRequestIdAndResponseStatus(Long requestId, String status);
}