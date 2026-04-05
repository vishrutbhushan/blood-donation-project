package com.G9.hemoconnect.repository;

import com.G9.hemoconnect.entity.Request;
import com.G9.hemoconnect.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {
    List<Response> findByRequestAndRepliedTrue(Request request);
    List<Response> findByRequest(Request request);
}
