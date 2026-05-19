package com.mkr.commerce.catalog.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mkr.commerce.common.exception.ApiException;
import com.mkr.commerce.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final boolean cloudinaryConfigured;

    public CloudinaryService(
            Cloudinary cloudinary,
            @Value("${app.cloudinary.cloud-name:}") String cloudName,
            @Value("${app.cloudinary.api-key:}")    String apiKey,
            @Value("${app.cloudinary.api-secret:}") String apiSecret
    ) {
        this.cloudinary = cloudinary;
        this.cloudinaryConfigured = !cloudName.isBlank() && !cloudName.startsWith("your-")
                && !apiKey.isBlank()    && !apiKey.startsWith("your-")
                && !apiSecret.isBlank() && !apiSecret.startsWith("your-");
    }

    public UploadResult upload(MultipartFile file, String folder) {
        return upload(file, folder, false);
    }

    public UploadResult upload(MultipartFile file, String folder, boolean isVideo) {
        if (!cloudinaryConfigured) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Media uploads are not available — Cloudinary credentials are not configured.",
                    ErrorCode.INTERNAL_ERROR);
        }
        try {
            Map<?, ?> result = isVideo
                    ? cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder",        folder,
                                    "resource_type", "video",
                                    "allowed_formats", new String[]{"mp4", "mov", "avi", "webm"}
                            ))
                    : cloudinary.uploader().upload(
                            file.getBytes(),
                            ObjectUtils.asMap(
                                    "folder",          folder,
                                    "resource_type",   "image",
                                    "allowed_formats", new String[]{"jpg", "jpeg", "png", "webp"}
                            ));
            return new UploadResult(
                    (String) result.get("secure_url"),
                    (String) result.get("public_id")
            );
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY,
                    "Media upload failed: " + e.getMessage(), ErrorCode.INTERNAL_ERROR);
        }
    }

    public void delete(String publicId) {
        if (publicId == null || publicId.isBlank()) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            // Non-fatal
        }
    }

    public record UploadResult(String url, String publicId) {}
}
