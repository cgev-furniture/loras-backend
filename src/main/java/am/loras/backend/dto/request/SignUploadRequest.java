package am.loras.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SignUploadRequest {

    @NotBlank
    private String contentType;

    private long fileSize;
}
