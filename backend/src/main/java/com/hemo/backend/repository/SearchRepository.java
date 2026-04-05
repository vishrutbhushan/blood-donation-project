package com.hemo.backend.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.Search;
import java.util.List;

public interface SearchRepository extends JpaRepository<Search, Long> {

    List<Search> findByUserUserId(Long userId);
}
