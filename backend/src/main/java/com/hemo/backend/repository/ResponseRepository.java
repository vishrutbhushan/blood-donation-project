package com.hemo.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.Response;    


public interface ResponseRepository extends JpaRepository<Response, Long> {}