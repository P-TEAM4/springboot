package com.lol.highlight.global.external.datadragon.entity;

import com.lol.highlight.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "data_dragon_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DataDragonVersion extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String version;

    @Column(nullable = false)
    private Boolean isActive;

    private LocalDateTime lastUpdatedAt;

    @Builder
    public DataDragonVersion(String version, Boolean isActive) {
        this.version = version;
        this.isActive = isActive != null ? isActive : false;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.lastUpdatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateVersion(String newVersion) {
        this.version = newVersion;
        this.lastUpdatedAt = LocalDateTime.now();
    }
}
