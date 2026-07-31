package com.fileupload.extblocker.upload;

import java.util.Set;

/**
 * Determines the real file format from content bytes only (PRD 4.2.1 step 1) —
 * the client-supplied MIME type / filename is never consulted for the signature match itself.
 *
 * exe/com/scr/cpl all share the Windows PE "MZ" header and are byte-for-byte
 * indistinguishable; likewise plain-text formats (js/bat/cmd/txt/...) have no magic
 * number at all. For those two families, a claimed extension that is a plausible
 * member of the matching family is accepted as the real extension (content can't
 * disprove it); anything outside the family falls back to a representative default,
 * which then mismatches the claim and correctly trips the spoofing check.
 */
final class FileSignatureDetector {

    private static final Set<String> PE_FAMILY = Set.of("exe", "com", "scr", "cpl");
    private static final Set<String> TEXT_FAMILY = Set.of(
            "txt", "csv", "json", "xml", "html", "htm", "css", "md",
            "log", "ini", "yml", "yaml", "js", "bat", "cmd", "sh");

    private FileSignatureDetector() {
    }

    static String detect(byte[] content, String claimedExtension) {
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) return "png";
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) return "jpg";
        if (startsWith(content, 0x47, 0x49, 0x46, 0x38)) return "gif";
        if (startsWith(content, 0x42, 0x4D)) return "bmp";
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46)) return "pdf";
        if (startsWith(content, 0x50, 0x4B, 0x03, 0x04)
                || startsWith(content, 0x50, 0x4B, 0x05, 0x06)
                || startsWith(content, 0x50, 0x4B, 0x07, 0x08)) return "zip";
        if (startsWith(content, 0x1F, 0x8B)) return "gz";
        if (startsWith(content, 0x7F, 0x45, 0x4C, 0x46)) return "elf";
        if (startsWith(content, 0x4D, 0x5A)) {
            return (claimedExtension != null && PE_FAMILY.contains(claimedExtension)) ? claimedExtension : "exe";
        }
        if (looksLikeText(content)) {
            return (claimedExtension != null && TEXT_FAMILY.contains(claimedExtension)) ? claimedExtension : "txt";
        }
        return "bin";
    }

    private static boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((content[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeText(byte[] content) {
        int sampleSize = Math.min(content.length, 8192);
        if (sampleSize == 0) {
            return true;
        }
        int printable = 0;
        for (int i = 0; i < sampleSize; i++) {
            int b = content[i] & 0xFF;
            if (b == 0) {
                return false;
            }
            if (b == 9 || b == 10 || b == 13 || (b >= 32 && b < 127) || b >= 128) {
                printable++;
            }
        }
        return printable >= sampleSize * 0.95;
    }
}
