package com.campus.trade.controller;

import com.campus.trade.common.ApiResponse;
import com.campus.trade.security.SecurityUtils;
import com.campus.trade.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public ApiResponse<FileService.UploadResult> upload(@RequestParam("file") MultipartFile file) {
        SecurityUtils.requireVerified();
        return ApiResponse.ok(fileService.upload(file));
    }
}
