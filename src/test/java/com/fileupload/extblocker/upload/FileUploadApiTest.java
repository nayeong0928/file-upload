package com.fileupload.extblocker.upload;

import com.fileupload.extblocker.extension.ExtensionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises POST /api/files/upload end-to-end against the real validation pipeline
 * (PRD 4.2.1) and the exact rejection wording from PRD 4.4.
 *
 * @Transactional rolls back any policy changes made via ExtensionService. Uploaded
 * physical files are not part of that transaction (plain disk I/O), so they're
 * swept up manually in an @AfterEach.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FileUploadApiTest {

    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 'I', 'H', 'D', 'R'
    };
    private static final byte[] PE_BYTES = {
            'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0, 4, 0, 0, 0, (byte) 0xFF, (byte) 0xFF, 0, 0
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExtensionService extensionService;

    @Value("${app.upload.dir}")
    private String uploadDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void cleanupUploadedFiles() throws IOException {
        Path dir = Path.of(uploadDir);
        if (Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                for (Path p : stream.toList()) {
                    Files.deleteIfExists(p);
                }
            }
        }
    }

    @Test
    void normalFileUploadSucceeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedExtension").value("png"))
                .andExpect(jsonPath("$.storedFilename").value("photo.png"))
                .andExpect(jsonPath("$.originalFilename").value("photo.png"));
    }

    @Test
    void blockedExtensionIsRejectedWithExactPolicyMessage() throws Exception {
        extensionService.setFixedBlocked("exe", true);
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", PE_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("차단된 확장자입니다: .exe"));
    }

    @Test
    void unblockedExtensionIsNotRejectedByPolicy() throws Exception {
        extensionService.setFixedBlocked("exe", false);
        MockMultipartFile file = new MockMultipartFile("file", "tool.exe", "application/octet-stream", PE_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detectedExtension").value("exe"));
    }

    @Test
    void doubleExtensionIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf.exe", "application/octet-stream", PNG_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("허용되지 않는 파일명입니다 (이중 확장자)"));
    }

    @Test
    void extensionSpoofingIsRejected() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", PE_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("파일의 실제 형식과 확장자가 일치하지 않습니다"));
    }

    @Test
    void extensionlessFilenameIsReplacedWithEpochSecondTimestamp() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "myphoto", "application/octet-stream", PNG_BYTES);
        long before = Instant.now().getEpochSecond();

        String storedFilename = uploadAndGetStoredFilename(file);
        long after = Instant.now().getEpochSecond();

        assertThat(storedFilename).endsWith(".png");
        long storedEpoch = Long.parseLong(storedFilename.substring(0, storedFilename.length() - ".png".length()));
        assertThat(storedEpoch).isBetween(before, after);
    }

    @Test
    void dotPrefixedFilenameIsReplacedRegardlessOfExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", ".env", "application/octet-stream", PNG_BYTES);
        long before = Instant.now().getEpochSecond();

        String storedFilename = uploadAndGetStoredFilename(file);
        long after = Instant.now().getEpochSecond();

        assertThat(storedFilename).endsWith(".png");
        long storedEpoch = Long.parseLong(storedFilename.substring(0, storedFilename.length() - ".png".length()));
        assertThat(storedEpoch).isBetween(before, after);
    }

    @Test
    void veryLongFilenameIsReplacedWithEpochSecondTimestamp() throws Exception {
        String longName = "a".repeat(150) + ".png";
        MockMultipartFile file = new MockMultipartFile("file", longName, "application/octet-stream", PNG_BYTES);

        String storedFilename = uploadAndGetStoredFilename(file);

        assertThat(storedFilename).endsWith(".png").isNotEqualTo(longName);
    }

    @Test
    void pathTraversalInFilenameIsNeutralized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../../../etc/passwd.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalFilename").value("passwd.png"))
                .andExpect(jsonPath("$.storedFilename").value("passwd.png"));
    }

    @Test
    void nullByteInFilenameIsRejected() throws Exception {
        String nameWithNullByte = "photo" + (char) 0 + ".exe";
        MockMultipartFile file = new MockMultipartFile("file", nameWithNullByte, "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isBadRequest());
    }

    private String uploadAndGetStoredFilename(MockMultipartFile file) throws Exception {
        String body = mockMvc.perform(multipart("/api/files/upload").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("storedFilename").asText();
    }
}
