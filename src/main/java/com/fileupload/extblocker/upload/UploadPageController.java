package com.fileupload.extblocker.upload;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * User-only entry point for the file upload screen (PRD 4.2).
 * Routing skeleton only for now — no upload UI/logic yet.
 */
@Controller
public class UploadPageController {

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload/index";
    }
}
