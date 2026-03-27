package am.loras.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateProjectRequest {

    @NotBlank
    private String title;

    private String description;

    private List<Long> categoryIds;

    private boolean published;

    private boolean featured;

    private String videoUrl;

    private List<String> imageObjectPaths;

    /** Nullable — auto-generated from title if blank. */
    private String slug;
}
