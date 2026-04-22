package com.hemo.backend.repository;

import com.hemo.backend.entity.Response;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResponseRepository extends JpaRepository<Response, Long> {

                @Query("""
                                                SELECT rp
                                                FROM Response rp
                                                JOIN FETCH rp.request r
                                                JOIN FETCH r.search s
                                                JOIN FETCH s.user
                                                WHERE s.user.userId = :userId
                                                        AND rp.responseStatus = 'YES'
                                                ORDER BY rp.respondedAt DESC
                                                """)
                List<Response> findAcceptedResponsesByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT rp
            FROM Response rp
            WHERE rp.request.requestId = :requestId
            ORDER BY rp.respondedAt DESC
            """)
    List<Response> findByRequestId(@Param("requestId") Long requestId);

    @Query("""
            SELECT rp.donorId
            FROM Response rp
            WHERE rp.request.search.searchId = :searchId
            """)
    List<String> findContactedDonorIdsBySearchId(@Param("searchId") Long searchId);

    @Query("""
            SELECT rp
            FROM Response rp
            WHERE rp.phoneNumber = :phoneNumber
                                                        AND rp.responseStatus IS NULL
            ORDER BY rp.respondedAt DESC
            """)
    List<Response> findPendingByPhone(@Param("phoneNumber") String phoneNumber);
}
