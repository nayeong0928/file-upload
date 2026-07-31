package com.fileupload.extblocker.upload;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * User-only entry point for the file upload screen (PRD 4.2).
 */
@Controller
public class UploadPageController {

    @GetMapping("/")
    public String root() {
        return "redirect:/upload";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload/index";
    }
}
