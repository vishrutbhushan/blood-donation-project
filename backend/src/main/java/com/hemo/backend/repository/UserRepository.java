package com.hemo.backend.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.hemo.backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
	Optional<User> findByPhone(String phone);
}
