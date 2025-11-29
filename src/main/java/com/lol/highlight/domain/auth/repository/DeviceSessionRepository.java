package com.lol.highlight.domain.auth.repository;

import com.lol.highlight.domain.auth.entity.DeviceSession;
import com.lol.highlight.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, Long> {

    Optional<DeviceSession> findByDeviceId(String deviceId);

    Optional<DeviceSession> findByRefreshToken(String refreshToken);

    Optional<DeviceSession> findByUserAndDeviceId(User user, String deviceId);

    List<DeviceSession> findByUser(User user);

    List<DeviceSession> findByUserAndRevokedFalse(User user);

    boolean existsByDeviceId(String deviceId);

    @Modifying
    @Query("DELETE FROM DeviceSession ds WHERE ds.refreshTokenExpiryDate < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE DeviceSession ds SET ds.revoked = true WHERE ds.user = :user AND ds.deviceId != :currentDeviceId")
    void revokeOtherDevices(@Param("user") User user, @Param("currentDeviceId") String currentDeviceId);

    @Modifying
    @Query("UPDATE DeviceSession ds SET ds.revoked = true WHERE ds.user = :user")
    void revokeAllDevices(@Param("user") User user);
}
