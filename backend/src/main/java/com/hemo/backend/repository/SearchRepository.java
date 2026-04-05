package com.hemo.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.Search;


public interface SearchRepository extends JpaRepository<Search, Long> {}
