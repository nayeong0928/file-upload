package com.fileupload.extblocker.extension;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ExtensionService {

    private static final int CUSTOM_EXTENSION_MAX_LENGTH = 20;
    private static final int CUSTOM_EXTENSION_MAX_COUNT = 200;
    private static final Pattern ALPHANUMERIC = Pattern.compile("^[A-Za-z0-9]+$");

    private final BlockedExtensionRepository repository;

    public ExtensionService(BlockedExtensionRepository repository) {
        this.repository = repository;
    }

    public List<BlockedExtension> getFixedExtensions() {
        return repository.findByTypeOrderByIdAsc(ExtensionType.FIXED);
    }

    public List<BlockedExtension> getCustomExtensions() {
        return repository.findByTypeOrderByIdAsc(ExtensionType.CUSTOM);
    }

    /**
     * Current blocked-extension set (checked FIXED ∪ all CUSTOM), queried fresh on every
     * call — FR-C1 forbids a cached policy that would delay picking up admin changes.
     */
    public Set<String> getCurrentlyBlockedExtensions() {
        Set<String> blocked = new HashSet<>();
        repository.findByType(ExtensionType.FIXED).stream()
                .filter(BlockedExtension::isBlocked)
                .map(BlockedExtension::getExtension)
                .forEach(blocked::add);
        repository.findByType(ExtensionType.CUSTOM).stream()
                .map(BlockedExtension::getExtension)
                .forEach(blocked::add);
        return blocked;
    }

    @Transactional
    public BlockedExtension setFixedBlocked(String extension, boolean blocked) {
        BlockedExtension fixed = repository.findByTypeAndExtensionIgnoreCase(ExtensionType.FIXED, extension)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 고정 확장자입니다"));
        fixed.setBlocked(blocked);
        return repository.save(fixed);
    }

    @Transactional
    public BlockedExtension addCustomExtension(String rawInput) {
        String normalized = normalize(rawInput);

        if (repository.existsByTypeAndExtensionIgnoreCase(ExtensionType.FIXED, normalized)) {
            throw new ExtensionValidationException("고정 확장자에 있는 확장자입니다");
        }
        if (repository.existsByTypeAndExtensionIgnoreCase(ExtensionType.CUSTOM, normalized)) {
            throw new ExtensionValidationException("이미 등록된 확장자입니다");
        }
        if (repository.countByType(ExtensionType.CUSTOM) >= CUSTOM_EXTENSION_MAX_COUNT) {
            throw new ExtensionValidationException("최대 200개까지 등록할 수 있습니다");
        }

        try {
            return repository.save(new BlockedExtension(normalized, ExtensionType.CUSTOM, true));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent requests can both pass the checks above and race
            // to insert; the DB-level unique index (case-insensitive) is the
            // real guard, this just turns that into the same user-facing message.
            throw new ExtensionValidationException("이미 등록된 확장자입니다");
        }
    }

    @Transactional
    public void deleteCustomExtension(Long id) {
        BlockedExtension custom = repository.findByIdAndType(id, ExtensionType.CUSTOM)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 커스텀 확장자입니다"));
        repository.delete(custom);
    }

    private String normalize(String rawInput) {
        String trimmed = rawInput == null ? "" : rawInput.trim();
        if (trimmed.isEmpty()
                || trimmed.length() > CUSTOM_EXTENSION_MAX_LENGTH
                || !ALPHANUMERIC.matcher(trimmed).matches()) {
            throw new ExtensionValidationException("영문/숫자만 입력할 수 있습니다");
        }
        return trimmed.toLowerCase();
    }
}
