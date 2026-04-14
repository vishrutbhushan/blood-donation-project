package com.hemo.backend.repository;

import com.hemo.backend.entity.Request;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("""
            SELECT COUNT(r)
            FROM Request r
            JOIN r.search s
            WHERE s.user.userId = :userId
              AND r.status = 'ACTIVE'
              AND r.expiresAt > CURRENT_TIMESTAMP
            """)
    long countActiveByUser(@Param("userId") Long userId);

    @Query("""
            SELECT r
            FROM Request r
            JOIN FETCH r.search s
            JOIN FETCH s.user
            WHERE r.requestId = :requestId
            """)
    Optional<Request> findByIdWithSearchAndUser(@Param("requestId") Long requestId);

    @Query("""
            SELECT r
            FROM Request r
            JOIN FETCH r.search s
            JOIN FETCH s.user
            WHERE s.user.userId = :userId
            ORDER BY r.createdAt DESC
            """)
    List<Request> findByUserId(@Param("userId") Long userId);
}
