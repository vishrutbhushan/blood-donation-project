package com.hemo.backend.repository;

import com.hemo.backend.entity.Response;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    @Query("""
            SELECT COUNT(rp)
            FROM Response rp
            WHERE rp.request.requestId = :requestId
              AND rp.respondedAt >= :since
            """)
    long countSince(@Param("requestId") Long requestId, @Param("since") LocalDateTime since);

    @Query("""
            SELECT COUNT(rp)
            FROM Response rp
            WHERE rp.request.requestId = :requestId
            """)
    long countByRequestId(@Param("requestId") Long requestId);

    @Query("""
            SELECT rp
            FROM Response rp
            JOIN FETCH rp.request r
            JOIN FETCH r.search s
            JOIN FETCH s.user
            WHERE s.user.userId = :userId
            ORDER BY rp.respondedAt DESC
            """)
    List<Response> findByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT rp
            FROM Response rp
            WHERE rp.request.requestId = :requestId
            ORDER BY rp.respondedAt DESC
            """)
    List<Response> findByRequestId(@Param("requestId") Long requestId);
}
