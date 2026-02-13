package com.lol.highlight.domain.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_settings")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "auto_launch", nullable = false)
    @Builder.Default
    private Boolean autoLaunch = false;

    @Column(name = "auto_show_on_lol", nullable = false)
    @Builder.Default
    private Boolean autoShowOnLol = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 설정 업데이트 메서드
    public void updateAutoLaunch(Boolean autoLaunch) {
        this.autoLaunch = autoLaunch;
    }

    public void updateAutoShowOnLol(Boolean autoShowOnLol) {
        this.autoShowOnLol = autoShowOnLol;
    }

    public void updateSettings(Boolean autoLaunch, Boolean autoShowOnLol) {
        this.autoLaunch = autoLaunch;
        this.autoShowOnLol = autoShowOnLol;
    }
}
