package com.lol.highlight.domain.session.repository;

import com.lol.highlight.domain.session.entity.DeviceSession;
import com.lol.highlight.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    Optional<DeviceSession> findByRefreshToken(String refreshToken);

    Optional<DeviceSession> findByDeviceId(String deviceId);

    Optional<DeviceSession> findByUserAndDeviceId(User user, String deviceId);

    List<DeviceSession> findAllByUser(User user);

    List<DeviceSession> findAllByUserOrderByLastAccessedAtDesc(User user);

    @Modifying
    @Query("DELETE FROM DeviceSession ds WHERE ds.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("DELETE FROM DeviceSession ds WHERE ds.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM DeviceSession ds WHERE ds.user = :user AND ds.deviceId = :deviceId")
    void deleteByUserAndDeviceId(@Param("user") User user, @Param("deviceId") String deviceId);

    long countByUser(User user);
}
