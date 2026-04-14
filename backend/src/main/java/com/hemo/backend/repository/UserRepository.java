package com.hemo.backend.repository;

import com.hemo.backend.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u WHERE u.abhaId = :abhaId")
    Optional<User> findByAbhaId(@Param("abhaId") String abhaId);
}
