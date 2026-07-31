package com.fileupload.extblocker.extension;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @Transactional so the "sh" row inserted below rolls back after the test instead of
// permanently polluting the shared in-memory DB that other test classes also read from.
@SpringBootTest
@Transactional
class BlockedExtensionUniqueConstraintTest {

    @Autowired
    private BlockedExtensionRepository repository;

    @Test
    void rejectsCaseInsensitiveDuplicateAtDbLevel() {
        repository.saveAndFlush(new BlockedExtension("sh", ExtensionType.CUSTOM, true));

        assertThatThrownBy(() ->
                repository.saveAndFlush(new BlockedExtension("SH", ExtensionType.CUSTOM, true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
