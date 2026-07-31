package com.fileupload.extblocker.extension;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlockedExtensionRepository extends JpaRepository<BlockedExtension, Long> {

    List<BlockedExtension> findByType(ExtensionType type);

    List<BlockedExtension> findByTypeOrderByIdAsc(ExtensionType type);

    Optional<BlockedExtension> findByExtensionIgnoreCase(String extension);

    Optional<BlockedExtension> findByTypeAndExtensionIgnoreCase(ExtensionType type, String extension);

    boolean existsByTypeAndExtensionIgnoreCase(ExtensionType type, String extension);

    long countByType(ExtensionType type);

    Optional<BlockedExtension> findByIdAndType(Long id, ExtensionType type);
}
