package com.campus.trade.service;

import com.campus.trade.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传：保存到本地上传目录，限制类型与大小。
 */
@Slf4j
@Service
public class FileService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp", "gif");
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    public UploadResult upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.badRequest("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw BizException.badRequest("图片大小不能超过 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw BizException.badRequest("只能上传图片文件");
        }
        String original = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw BizException.badRequest("仅支持 png、jpg、jpeg、webp、gif 格式的图片");
        }

        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path directory = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(datePath);
        try {
            Files.createDirectories(directory);
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(Paths.get(uploadDir).toAbsolutePath().normalize())) {
                throw BizException.badRequest("非法的文件路径");
            }
            file.transferTo(target);
        } catch (IOException ex) {
            log.error("图片保存失败", ex);
            throw new BizException(com.campus.trade.common.ErrorCode.SERVER_ERROR, "图片保存失败，请稍后重试");
        }
        String url = "/uploads/" + datePath + "/" + filename;
        return new UploadResult(url, original);
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record UploadResult(String url, String name) {
    }
}
