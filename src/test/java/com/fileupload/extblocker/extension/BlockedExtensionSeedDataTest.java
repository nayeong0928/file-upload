package com.fileupload.extblocker.extension;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BlockedExtensionSeedDataTest {

    private static final List<String> FIXED_EXTENSIONS =
            List.of("bat", "cmd", "com", "cpl", "exe", "scr", "js");

    @Autowired
    private BlockedExtensionRepository repository;

    @Test
    void fixedExtensionsAreSeededAsUnblocked() {
        List<BlockedExtension> fixed = repository.findByType(ExtensionType.FIXED);

        assertThat(fixed).hasSize(7);
        assertThat(fixed.stream().map(BlockedExtension::getExtension).toList())
                .containsExactlyInAnyOrderElementsOf(FIXED_EXTENSIONS);
        assertThat(fixed).allMatch(e -> !e.isBlocked());
    }
}
