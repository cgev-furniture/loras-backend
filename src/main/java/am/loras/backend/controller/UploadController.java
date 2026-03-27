package am.loras.backend.controller;

import am.loras.backend.dto.request.SignUploadRequest;
import am.loras.backend.dto.response.SignedUrlResponse;
import am.loras.backend.exception.ValidationException;
import am.loras.backend.service.StorageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/upload")
@PreAuthorize("hasRole('ADMIN')")
public class UploadController {

    private static final long MAX_IMAGE_SIZE = 10L * 1024 * 1024;  // 10 MB
    private static final long MAX_VIDEO_SIZE = 30L * 1024 * 1024;  // 30 MB
    private static final Duration SIGNED_URL_TTL = Duration.ofMinutes(15);

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );

    private final StorageService storageService;

    public UploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    /**
     * POST /api/v1/upload/image/sign
     * Validates content type and file size, then returns a V4 signed PUT URL for a GCS image upload.
     */
    @PostMapping("/image/sign")
    public ResponseEntity<SignedUrlResponse> signImageUpload(@Valid @RequestBody SignUploadRequest req) {
        if (!ALLOWED_IMAGE_TYPES.contains(req.getContentType())) {
            throw new ValidationException(
                    "Unsupported image content type: " + req.getContentType()
                    + ". Allowed: image/jpeg, image/png, image/webp");
        }
        if (req.getFileSize() > MAX_IMAGE_SIZE) {
            throw new ValidationException("Image file size exceeds the 10 MB limit");
        }

        String extension = extensionForImageType(req.getContentType());
        String objectPath = "images/" + UUID.randomUUID() + "." + extension;

        SignedUrlResponse response = storageService.generateSignedUploadUrl(
                objectPath, req.getContentType(), SIGNED_URL_TTL);

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/v1/upload/video/sign
     * Validates content type and file size, then returns a V4 signed PUT URL for a GCS video upload.
     */
    @PostMapping("/video/sign")
    public ResponseEntity<SignedUrlResponse> signVideoUpload(@Valid @RequestBody SignUploadRequest req) {
        if (!"video/mp4".equals(req.getContentType())) {
            throw new ValidationException(
                    "Unsupported video content type: " + req.getContentType()
                    + ". Only video/mp4 is allowed");
        }
        if (req.getFileSize() > MAX_VIDEO_SIZE) {
            throw new ValidationException("Video file size exceeds the 30 MB limit");
        }

        String objectPath = "videos/" + UUID.randomUUID() + ".mp4";

        SignedUrlResponse response = storageService.generateSignedUploadUrl(
                objectPath, req.getContentType(), SIGNED_URL_TTL);

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String extensionForImageType(String contentType) {
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }
}
