package com.fileupload.extblocker.upload;

import com.fileupload.extblocker.upload.dto.UploadSuccessResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping(path = "/upload", consumes = "multipart/form-data")
    public UploadSuccessResponse upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new UploadRejectedException("업로드할 파일을 첨부해주세요");
        }
        return fileUploadService.upload(file);
    }
}
