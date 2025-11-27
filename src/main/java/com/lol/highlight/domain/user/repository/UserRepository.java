package com.lol.highlight.domain.user.repository;

import com.lol.highlight.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByRiotId(String riotId);

    boolean existsByEmail(String email);

    boolean existsByRiotId(String riotId);
}
