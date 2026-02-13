package com.lol.highlight.global.external.datadragon.repository;

import com.lol.highlight.global.external.datadragon.entity.DataDragonVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DataDragonVersionRepository extends JpaRepository<DataDragonVersion, Long> {

    Optional<DataDragonVersion> findByIsActiveTrue();

    Optional<DataDragonVersion> findByVersion(String version);
}
