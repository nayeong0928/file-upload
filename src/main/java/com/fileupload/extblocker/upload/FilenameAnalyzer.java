package com.fileupload.extblocker.upload;

import java.nio.charset.StandardCharsets;

/**
 * Parses a raw uploaded filename per PRD 4.2.1 without touching file content.
 * A dot-prefixed name (e.g. ".env") is treated as having no extension: the leading
 * dot is the hidden-file marker, not a separator, per PRD 8.7's assumption.
 */
final class FilenameAnalyzer {

    private static final int MAX_BASENAME_LENGTH = 100;
    private static final int MAX_FILENAME_BYTES = 255;

    private final String basename;
    private final boolean startsWithDot;
    private final int extensionTokenCount;
    private final String claimedExtension;

    private FilenameAnalyzer(String basename, boolean startsWithDot, int extensionTokenCount, String claimedExtension) {
        this.basename = basename;
        this.startsWithDot = startsWithDot;
        this.extensionTokenCount = extensionTokenCount;
        this.claimedExtension = claimedExtension;
    }

    static FilenameAnalyzer analyze(String rawFilename) {
        String name = stripDirectory(rawFilename);
        boolean leadingDot = name.startsWith(".");
        String core = leadingDot ? name.substring(1) : name;
        String[] parts = core.split("\\.", -1);
        int tokenCount = Math.max(parts.length - 1, 0);

        String claimed = null;
        if (tokenCount >= 1) {
            String last = parts[parts.length - 1];
            if (!last.isEmpty()) {
                claimed = last.toLowerCase();
            }
        }
        return new FilenameAnalyzer(name, leadingDot, tokenCount, claimed);
    }

    private static String stripDirectory(String rawFilename) {
        String name = rawFilename == null ? "" : rawFilename;
        int lastSeparator = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        return lastSeparator >= 0 ? name.substring(lastSeparator + 1) : name;
    }

    boolean isDoubleExtension() {
        return extensionTokenCount >= 2;
    }

    boolean hasExtension() {
        return claimedExtension != null;
    }

    String claimedExtension() {
        return claimedExtension;
    }

    boolean isTooLong() {
        String withoutExtension = hasExtension()
                ? basename.substring(0, basename.length() - claimedExtension.length() - 1)
                : basename;
        boolean baseTooLong = withoutExtension.length() > MAX_BASENAME_LENGTH;
        boolean totalTooLong = basename.getBytes(StandardCharsets.UTF_8).length > MAX_FILENAME_BYTES;
        return baseTooLong || totalTooLong;
    }

    boolean needsSpecialNaming() {
        return !hasExtension() || startsWithDot || isTooLong();
    }

    String basename() {
        return basename;
    }
}
