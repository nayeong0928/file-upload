package com.fileupload.extblocker.upload;

import com.fileupload.extblocker.extension.ExtensionService;
import com.fileupload.extblocker.upload.dto.UploadSuccessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Service
public class FileUploadService {

    private static final char NULL_BYTE = (char) 0;

    private final ExtensionService extensionService;
    private final UploadFileRepository uploadFileRepository;
    private final Path storageDir;

    public FileUploadService(ExtensionService extensionService,
                              UploadFileRepository uploadFileRepository,
                              @Value("${app.upload.dir:uploads-data}") String uploadDir) {
        this.extensionService = extensionService;
        this.uploadFileRepository = uploadFileRepository;
        this.storageDir = Path.of(uploadDir);
        try {
            Files.createDirectories(storageDir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public UploadSuccessResponse upload(MultipartFile file) {
        String rawFilename = file.getOriginalFilename();

        // NFR 5장: null byte injection — reject before any path/name derivation touches it.
        if (rawFilename != null && rawFilename.indexOf(NULL_BYTE) >= 0) {
            throw new UploadRejectedException("허용되지 않는 파일명입니다");
        }

        FilenameAnalyzer analyzer = FilenameAnalyzer.analyze(rawFilename);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Step 1: real format from content only — client MIME type is never consulted.
        String realExtension = FileSignatureDetector.detect(content, analyzer.claimedExtension());

        // Step 2: double extension.
        if (analyzer.isDoubleExtension()) {
            reject(analyzer, realExtension, "허용되지 않는 파일명입니다 (이중 확장자)");
        }

        // Step 3: extension spoofing — skipped when the filename has no extension.
        if (analyzer.hasExtension() && !analyzer.claimedExtension().equals(realExtension)) {
            reject(analyzer, realExtension, "파일의 실제 형식과 확장자가 일치하지 않습니다");
        }

        // Step 4: current blocked policy, queried fresh (FR-C1), checked on the real extension.
        Set<String> blockedExtensions = extensionService.getCurrentlyBlockedExtensions();
        if (blockedExtensions.contains(realExtension)) {
            reject(analyzer, realExtension, "차단된 확장자입니다: ." + realExtension);
        }

        // Step 5: special-filename substitution — only for files that already passed 1-4.
        String logicalStoredName = analyzer.needsSpecialNaming()
                ? Instant.now().getEpochSecond() + "." + realExtension
                : analyzer.basename();

        // FR-C4: the physical path is derived only from a generated UUID, never from user input.
        String physicalName = UUID.randomUUID() + "." + realExtension;
        Path physicalPath = storageDir.resolve(physicalName);
        try {
            Files.write(physicalPath, content);
            restrictPermissionsIfSupported(physicalPath);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        uploadFileRepository.save(new UploadFile(
                analyzer.basename(), logicalStoredName, realExtension, UploadStatus.SUCCESS, null));

        return new UploadSuccessResponse(logicalStoredName, analyzer.basename(), realExtension);
    }

    private void reject(FilenameAnalyzer analyzer, String realExtension, String reason) {
        uploadFileRepository.save(new UploadFile(
                analyzer.basename(), analyzer.basename(), realExtension, UploadStatus.REJECTED, reason));
        throw new UploadRejectedException(reason);
    }

    private void restrictPermissionsIfSupported(Path path) throws IOException {
        try {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rw-r-----"));
        } catch (UnsupportedOperationException notPosix) {
            // Non-POSIX filesystem (e.g. Windows) — no executable bit to strip in the first place.
        }
    }
}
