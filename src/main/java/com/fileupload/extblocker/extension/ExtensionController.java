package com.fileupload.extblocker.extension;

import com.fileupload.extblocker.extension.dto.CustomExtensionRequest;
import com.fileupload.extblocker.extension.dto.CustomExtensionResponse;
import com.fileupload.extblocker.extension.dto.FixedExtensionResponse;
import com.fileupload.extblocker.extension.dto.PatchFixedRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/extensions")
public class ExtensionController {

    private final ExtensionService extensionService;

    public ExtensionController(ExtensionService extensionService) {
        this.extensionService = extensionService;
    }

    @GetMapping("/fixed")
    public List<FixedExtensionResponse> getFixedExtensions() {
        return extensionService.getFixedExtensions().stream()
                .map(FixedExtensionResponse::from)
                .toList();
    }

    @PatchMapping("/fixed/{extension}")
    public FixedExtensionResponse patchFixedExtension(@PathVariable String extension,
                                                        @RequestBody PatchFixedRequest request) {
        BlockedExtension updated = extensionService.setFixedBlocked(extension, request.isBlocked());
        return FixedExtensionResponse.from(updated);
    }

    @GetMapping("/custom")
    public List<CustomExtensionResponse> getCustomExtensions() {
        return extensionService.getCustomExtensions().stream()
                .map(CustomExtensionResponse::from)
                .toList();
    }

    @PostMapping("/custom")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomExtensionResponse addCustomExtension(@RequestBody CustomExtensionRequest request) {
        BlockedExtension created = extensionService.addCustomExtension(request.extension());
        return CustomExtensionResponse.from(created);
    }

    @DeleteMapping("/custom/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCustomExtension(@PathVariable Long id) {
        extensionService.deleteCustomExtension(id);
    }
}
