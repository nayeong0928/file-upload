package com.fileupload.extblocker.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Admin-only entry point for the extension policy screen (PRD 4.1).
 * Routing skeleton only for now — no policy UI/logic yet.
 */
@Controller
public class AdminExtensionPageController {

    @GetMapping("/admin/extensions")
    public String extensionsPage() {
        return "admin/extensions";
    }
}
